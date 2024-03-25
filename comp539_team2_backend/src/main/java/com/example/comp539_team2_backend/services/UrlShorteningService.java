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


@Service
public class UrlShorteningService {
    @Autowired
    private BigtableRepository urlTableRepository;
    String prefix = "https://snaplk.com/";

    //Base62 characters set to encode ID
    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

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
    public String shorten_url(String long_url, String key) throws Exception {
        //Generate a row key from the original URL
        String rowKey = generateRowKey(long_url);

        //Check existing shorten URL
        String existingUrl = urlTableRepository.get(rowKey, "url", "originalUrl");
        if (existingUrl != null) {
            return buildShortUrl(rowKey);
        }

        // Save the new shortened URL information in Bigtable
        urlTableRepository.save(rowKey, "url", "originalUrl", long_url);
        urlTableRepository.save(rowKey, "url", "shortenedUrl", buildShortUrl(rowKey));

        // Save the expired time information in Bigtable
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Set create time
        Date currentDate = calendar.getTime();
        String createdAt = sdf.format(currentDate);
        urlTableRepository.save(rowKey, "url", "createdAt", createdAt);

        // Set expired time
        if (key.equals("0")) {
            calendar.add(Calendar.DATE, 365);
        }

        Date futureDate = calendar.getTime();
        String expiredAt = sdf.format(futureDate);
        urlTableRepository.save(rowKey, "url", "expiredAt", expiredAt);

        return buildShortUrl(rowKey);
    }

    public String resolve_url(String shortened_url) throws Exception {
        // Extract the row key from the shortened URL
        String rowKey = shortened_url.substring(shortened_url.lastIndexOf("/") + 1);

        return urlTableRepository.get(rowKey, "url", "originalUrl");
    }

    public String buildShortUrl(String rowKey) {
        return prefix + rowKey;
    }

    //Advanced functions for premium users
    public String customized_url(String long_url, String customized_url, String key) throws Exception {
        if (!key.equals("0") && customized_url != null) {
            String rowKey = customized_url.replace(prefix, "");
            String shortened_url = urlTableRepository.get(rowKey, "url", "originalUrl");

            if (shortened_url == null) {
                urlTableRepository.save(rowKey, "url", "originalUrl", long_url);
                return buildShortUrl(rowKey);
            } else {
                throw new Exception("Customized URL is already in use. Please try a different URL.");
            }
        } else {
            // Handle the case where the key is "0" or the customized URL is null
            throw new Exception("No right to use customized url functionality");
        }
    }


    public List<String> bulk_shorten_urls(String[] long_urls, String key) throws Exception {
        List<String> shortened_urls = new ArrayList<>();
        if (!key.equals("0")) {
            for (String long_url : long_urls) {
                shortened_urls.add(shorten_url(long_url, key));
            }
        }
        return shortened_urls;
    }

    public List<String> bulk_resolve_urls(String[] shortened_urls, String key) throws Exception {
        List<String> original_urls = new ArrayList<>();
        if (!key.equals("0")) {
            for (String shortened_url : shortened_urls) {
                original_urls.add(resolve_url(shortened_url));
            }
        }
        return original_urls;
    }

    public boolean renew_url_expiration(String short_url, String key) throws IOException {
        String rowKey = short_url.replace(prefix, "");
        if (key.equals("1")) {
            urlTableRepository.deleteColumn(rowKey, "url", "expiredAt");
            return true;
        }

        return false;
    }

    public boolean delete_url(String short_url, String key) throws IOException {
        String rowKey = short_url.replace(prefix, "");
        if (key.equals("1")) {
            urlTableRepository.deleteRow(rowKey);
            return true;
        }

        return false;
    }

}
