package com.example.comp539_team2_backend.services;

import com.example.comp539_team2_backend.configs.BigtableRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;


@Service
public class UrlShorteningService {
    @Autowired
    private BigtableRepository urlTableRepository;

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

        // 6 bytes (48 bits) from the hash would be enough. Let's take the first 6 bytes for simplicity.
        byte[] hashPrefix = Arrays.copyOfRange(sha256hash, 0, 6);

        // If exactly 8 characters are needed, additional logic might be required to ensure the result length.
        StringBuilder encoded = new StringBuilder(encodeBase62(hashPrefix));

        // Ensure the string length is 8 by padding with a character, e.g., '0', if necessary.
        while (encoded.length() < 8) {
            encoded.insert(0, "0");
        }

        return encoded.toString();
    }

    //Basic functions for general users
    public String shorten_url(String long_url) throws Exception {
        System.out.println("long_url = " + long_url);
        //Generate a row key from the original URL
        String rowKey = generateRowKey(long_url);

        //Check existing shorten URL
        String existingUrl = urlTableRepository.get(rowKey, "url", "originalUrl");
        if (existingUrl != null) {
            return buildShortUrl(rowKey);
        }

        // Save the new shortened URL information in Bigtable
        urlTableRepository.save(rowKey, "url", "originalUrl", long_url);

        // Save the expired time information in Bigtable
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 14);
        Date futureDate = calendar.getTime();

        //Formatting
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String expiredAt = sdf.format(futureDate);
        urlTableRepository.save(rowKey, "url", "expiredAt", expiredAt);

        //TODO: premium key and default key

        return buildShortUrl(rowKey);
    }

    public String resolve_url(String shortened_url) throws Exception {
        // Extract the row key from the shortened URL
        String rowKey = shortened_url.substring(shortened_url.lastIndexOf("/") + 1);

        return urlTableRepository.get(rowKey, "url", "originalUrl");
    }

    public String buildShortUrl(String rowKey) {
        return "https://snaplk.com/" + rowKey;
    }

    //Advanced functions for premium users
    public String shorten_url(String long_url, String customized_url, String key) {
        return "";
    }

    public String[] bulk_shorten_urls(String[] long_urls, String key) {
        String[] res = new String[long_urls.length];
        return res;
    }

    public String[] bulk_resolve_urls(String[] shortend_urls, String key) {
        String[] res = new String[shortend_urls.length];
        return res;
    }


}
