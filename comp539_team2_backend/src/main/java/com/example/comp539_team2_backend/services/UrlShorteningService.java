package com.example.comp539_team2_backend.services;

import com.example.comp539_team2_backend.configs.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UrlShorteningService {
    @Autowired
    private BigtableRepository urlTableRepository;

    String prefix = "https://snaplk.com/";
    private static final Logger logger = LoggerFactory.getLogger(UrlShorteningService.class);

    //Base62 characters set to encode ID
    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static final int CURRENT_DATE = 0;
    public static final int ONE_YEAR = 365;
    public static final int FOREVER = 999;

    boolean is_premium (String email)throws IOException
    {
        String subStatus = urlTableRepository.get(email, "user", "subscription");
        boolean isPremium = false;

        if (subStatus == null || subStatus.equals("0")) {
            isPremium = false;
        } else {
            isPremium = true;
        }
        return isPremium;
    }

    private String encodeBase62(byte[] input) {
        if (input == null) {
            throw new IllegalArgumentException("Cannot encode a null array.");
        }

        BigInteger number = new BigInteger(1, input); // Ensure the number is positive
        StringBuilder base62 = new StringBuilder();
        BigInteger base = BigInteger.valueOf(62);
        while (number.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divmod = number.divideAndRemainder(base);
            base62.insert(0, BASE62.charAt(divmod[1].intValue()));
            number = divmod[0];
        }
        return base62.toString();
    }

    public String generateRowKey(String originalUrl) {
        if (originalUrl == null) {
            throw new IllegalArgumentException("Original URL must not be null.");
        }

        byte[] sha256hash = DigestUtils.sha256(originalUrl);

        // 6 bytes from the hash would be enough for uniqueness.
        byte[] hashPrefix = Arrays.copyOfRange(sha256hash, 0, 6);

        // Base62 encode the 6 byte hash prefix.
        String encoded = encodeBase62(hashPrefix);

        // Ensure the encoded string length is exactly 8 by padding with '0' or truncating if necessary.
        if (encoded.length() < 8) {
            // Pad with '0' from the left.
            encoded = String.format("%8s", encoded).replace(' ', '0');
        } else if (encoded.length() > 8) {
            // Truncate to 8 characters.
            encoded = encoded.substring(0, 8);
        }

        return encoded;
    }

    //Basic functions for general users
    public String shorten_url(String long_url, String email) throws Exception {
        //Generate a row key from the original URL
        String rowKey = generateRowKey(long_url);
        boolean isPremium = false;
        String subStatus = urlTableRepository.get(email, "user", "subscription");

        if (subStatus != null && subStatus.equals("1")) {
            isPremium = true;
        }

        //Check existing shorten URL
        String existingUrl = urlTableRepository.get(rowKey, "url", "originalUrl");
        if (existingUrl != null) {
            return buildShortUrl(rowKey);
        }

        // Save the new shortened URL information in Bigtable
        urlTableRepository.save(rowKey, "url", "originalUrl", long_url);
        urlTableRepository.save(rowKey, "url", "shortenedUrl", buildShortUrl(rowKey));

        // Save the creator info for logging user
        if (subStatus == null) {
            urlTableRepository.save(rowKey, "url", "creator", "NO_USER");
        } else {
            urlTableRepository.save(rowKey, "url", "creator", email);
        }

        urlTableRepository.save(rowKey, "url", "createdAt", getDate(CURRENT_DATE));

        // Set expired time
        if (isPremium) {
            urlTableRepository.save(rowKey, "url", "expiredAt", "NEVER");
        } else {
            urlTableRepository.save(rowKey, "url", "expiredAt", getDate(ONE_YEAR));
        }
        urlTableRepository.save(rowKey, "url", "spam","0");
        return buildShortUrl(rowKey);
    }

    public String resolve_url(String shortened_url) throws Exception {
        // Extract the row key from the shortened URL
        String rowKey = shortened_url.substring(shortened_url.lastIndexOf("/") + 1);
        logger.info("Resolving shortened URL: {}", shortened_url);
        logger.debug("Extracted rowKey: {}", rowKey);
        return urlTableRepository.get(rowKey, "url", "originalUrl");
    }

    public String buildShortUrl(String rowKey) {
        return prefix + rowKey;
    }

    //Advanced functions for premium users
    public String customized_url(String long_url, String customized_url, String email) throws Exception {

        String subStatus = urlTableRepository.get(email, "user", "subscription");
        boolean isPremium = false;

        if (subStatus == null || subStatus.equals("0")) {
            isPremium = false;
        } else {
            isPremium = true;
        }


        if (isPremium && customized_url != null) {
            String rowKey = customized_url.replace(prefix, "");
            String shortened_url = urlTableRepository.get(rowKey, "url", "originalUrl");

            if (shortened_url == null) {
                urlTableRepository.save(rowKey, "url", "originalUrl", long_url);
                urlTableRepository.save(rowKey, "url", "shortenedUrl", buildShortUrl(rowKey));
                urlTableRepository.save(rowKey, "url", "createdAt", getDate(CURRENT_DATE));
                urlTableRepository.save(rowKey, "url", "expiredAt", getDate(FOREVER));
                urlTableRepository.save(rowKey, "url", "creator", email);
                return buildShortUrl(rowKey);
            } else {
                throw new Exception("Customized URL is already in use. Please try a different URL.");
            }
        } else {
            // Handle the case where the key is "0" or the customized URL is null
            throw new Exception("No right to use customized url functionality");
        }
    }

    public List<String> bulk_shorten_urls(String[] long_urls, String email) throws Exception {
        List<String> shortened_urls = new ArrayList<>();
        boolean premium= is_premium(email);
        if (premium) {
            for (String long_url : long_urls) {
                logger.info("Email: " + email);
                shortened_urls.add(shorten_url(long_url, email));
            }
        } else {
            return shortened_urls;

        }
        return shortened_urls;
    }

    public List<String> bulk_resolve_urls(String[] shortened_urls, String email) throws Exception {
        List<String> original_urls = new ArrayList<>();
        boolean premium= is_premium(email);
        if (premium) {
            for (String shortened_url : shortened_urls) {
                original_urls.add(resolve_url(shortened_url));
            }
        }
        return original_urls;
    }

    public boolean renew_url_expiration(String email) throws IOException {
        
        boolean premium= is_premium(email);
        if (premium) {
            urlTableRepository.updateExpiration(email);
            return true;
        }
        return false;
    }

    public boolean delete_url(String short_url, String email) throws IOException {
        String rowKey = short_url.replace(prefix, "");

        boolean isSameCreator = false;
        String creator = urlTableRepository.get(rowKey, "url", "creator");

        if (creator == null || !creator.equals(email)) {
            isSameCreator = false;
        } else {
            isSameCreator = true;
        }

        boolean premium = is_premium(email);

        if (premium && isSameCreator) {
            urlTableRepository.deleteRow(rowKey);
            return true;
        }

        return false;
    }

    public String getDate(int date) {
        // Save the expired time information in Bigtable
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Set create time
        Date currentDate = calendar.getTime();

        if (date == CURRENT_DATE) {
            return sdf.format(currentDate);
        } else if (date == ONE_YEAR) {
            calendar.add(Calendar.DATE, 365);
            Date futureDate = calendar.getTime();
            return sdf.format(futureDate);
        } else if (date == FOREVER) {
            return "NEVER";
        }

        return "NEVER";
    }

    public boolean mark_url_as_spam(String short_url,String email) throws IOException {
        boolean is_marked = false;
        String rowKey = short_url.replace(prefix, "");
        boolean premium = is_premium(email);
        if (premium) {
           is_marked=urlTableRepository.save_a(rowKey, "url", "spam","1");
        }
        return is_marked;
    }
    public boolean remove_spam(String short_url,String email) throws IOException {
        boolean is_unmarked = false;
        String rowKey = short_url.replace(prefix, "");
        boolean premium = is_premium(email);
        if (premium) {
            is_unmarked=urlTableRepository.save_a(rowKey, "url", "spam","0");
        }
        return is_unmarked;
    }

    public Map<String,String> get_info(String short_url,String email)throws IOException{
        Map<String, String> information 
            = new HashMap<String,String>();  
        String rowKey = short_url.replace(prefix, "");
        boolean premium = is_premium(email);
        if (premium) {
            String longUrl = urlTableRepository.get(rowKey, "url", "originalUrl");
        information.put("long_url",longUrl);
        String created_at=urlTableRepository.get(rowKey, "url", "createdAt");
        information.put("created_at",created_at);
        String expires_at=urlTableRepository.get(rowKey, "url", "expiredAt");
        information.put("expires_at",expires_at);
        String spam=urlTableRepository.get(rowKey, "url", "spam");
        information.put("spam_status",spam);
        }
        return information;
    }
    public List<String> get_history(String email)throws IOException{
        List<String> short_urls = new ArrayList<>();
        boolean premium= is_premium(email);
        if(premium)
        {
            short_urls=urlTableRepository.getHistory(email);
        }
        return short_urls;
    }
}
