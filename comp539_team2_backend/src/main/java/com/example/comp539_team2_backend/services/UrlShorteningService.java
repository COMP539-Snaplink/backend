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

    String prefix = "https://snaplink.surge.sh/";
    private static final Logger logger = LoggerFactory.getLogger(UrlShorteningService.class);

    //Base62 characters set to encode ID
    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static final int CURRENT_DATE = 0;
    public static final int ONE_YEAR = 365;
    public static final int FOREVER = 999;



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
        byte[] hashPrefix = Arrays.copyOfRange(sha256hash, 0, 8);

        // Base62 encode the 6 byte hash prefix.
        String encoded = encodeBase62(hashPrefix);

        // Ensure the encoded string length is exactly 8 by padding with '0' or truncating if necessary.
        return encoded.length() > 8 ? encoded.substring(0, 8) : encoded;
    }

    // Check if the user is a premium user
    private boolean isPremiumUser(String email) {
        String subStatus = urlTableRepository.get(email, "user", "subscription");
        return subStatus != null && subStatus.equals("1");
    }

    public String buildShortUrl(String rowKey) {
        return prefix + rowKey;
    }

    //Basic functions for general users
    // Main function to shorten URLs
    public String shorten_url(String longUrl, String email) throws Exception {
        // Generate a base row key from the original URL
        String baseRowKey = generateRowKey(longUrl);

        // Check if the user is a premium user
        boolean isPremium = isPremiumUser(email);

        // Handle hash collision and check if the long URL exists in the database
        int attempt = 0, maxAttempts = 100;
        String rowKey = baseRowKey;
        while (attempt < maxAttempts) { // Assuming 100 as maxAttempts
            String existingUrl = urlTableRepository.get(rowKey, "url", "originalUrl");
            if (existingUrl == null) {
                // If there's no conflict, can use the current rowKey
                break;
            } else if (existingUrl.equals(longUrl)) {
                // Found the same long URL, return the corresponding short URL
                return buildShortUrl(rowKey);
            } else {
                // Hash collision occurred, try the next rowKey
                attempt++;
                rowKey = baseRowKey + "_" + attempt;
            }
        }

        // If the maximum number of attempts is reached and the conflict is still not resolved, throw an exception
        if (attempt == maxAttempts) {
            throw new RuntimeException("Unable to resolve hash collision after " + maxAttempts + " attempts.");
        }

        // Save the new shortened URL information in the database
        urlTableRepository.save(rowKey, "url", "originalUrl", longUrl);
        urlTableRepository.save(rowKey, "url", "shortenedUrl", buildShortUrl(rowKey));

        // Save the creator's information
        urlTableRepository.save(rowKey, "url", "creator", isPremium ? email : "NO_USER");
        urlTableRepository.save(rowKey, "url", "createdAt", getDate(CURRENT_DATE));

        // Set the expiration time for the shortened URL
        urlTableRepository.save(rowKey, "url", "expiredAt", isPremium ? "NEVER" : getDate(ONE_YEAR));

        // Return the shortened URL
        return buildShortUrl(rowKey);
    }


    public String resolve_url(String shortened_url) throws Exception {
        // Extract the row key from the shortened URL
        String rowKey = shortened_url.substring(shortened_url.lastIndexOf("/") + 1);
        logger.info("Resolving shortened URL: {}", shortened_url);
        logger.debug("Extracted rowKey: {}", rowKey);
        return urlTableRepository.get(rowKey, "url", "originalUrl");
    }

    //Advanced functions for premium users
    public String customized_url(String long_url, String customized_url, String email) throws Exception {
        boolean isPremium = isPremiumUser(email);

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
        boolean isPremium = isPremiumUser(email);

        if (isPremium) {
            for (String long_url : long_urls) {
                logger.info("Email: " + email);
                shortened_urls.add(shorten_url(long_url, email));
            }
        } else {
            throw new Exception("No right to bulk shorten urls");
        }
        return shortened_urls;
    }

    public List<String> bulk_resolve_urls(String[] shortened_urls, String email) throws Exception {
        List<String> original_urls = new ArrayList<>();
        boolean isPremium = isPremiumUser(email);

        if (isPremium) {
            for (String shortened_url : shortened_urls) {
                original_urls.add(resolve_url(shortened_url));
            }
        } else {
            throw new Exception("No right to bulk resolve urls");
        }

        return original_urls;
    }

    public boolean renew_url_expiration(String email) throws IOException {
        boolean isPremium = isPremiumUser(email);

        if (isPremium) {
            urlTableRepository.updateExpiration(email);
            return true;
        }

        return false;
    }

    public boolean delete_url(String short_url, String email) throws IOException {
        String rowKey = short_url.replace(prefix, "");

        boolean isPremium = isPremiumUser(email);
        boolean isSameCreator = false;
        String creator = urlTableRepository.get(rowKey, "url", "creator");

        if (creator == null || !creator.equals(email)) {
            isSameCreator = false;
        } else {
            isSameCreator = true;
        }

        if (isPremium && isSameCreator) {
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
}
