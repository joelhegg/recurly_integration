package za.co.trf.recurly.js;

import org.apache.log4j.Logger;
import za.co.trf.recurly.*;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Java implementation of recurly_js.php
 */
public class RecurlyJS {

    private static final Logger log = Logger.getLogger(RecurlyJS.class);

    private KeyProvider keyProvider;

    public RecurlyJS(String privateKey) {
        keyProvider = new SimpleKeyProvider(privateKey);
    }

    public RecurlyJS(KeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    /**
     * Convenience method for signing a typical new plan subscription
     * @param planCode the code of the plan to be subscribed to
     * @return the generated signature
     */
    public String signForNewSubscriptionToPlan(final String planCode) {
        return sign(new HashMap<String, Object>() {{
            put(RecurlyConstants.JS_PARAM_SUBSCRIPTION, new HashMap<String, Object>() {
                {
                    put(RecurlyConstants.JS_PARAM_PLAN_CODE, planCode);
                }
            });
        }});
    }

    /**
     * Convenience method for signing a typical billing information update
     * @param accountCode the account code who billing information is to be updated
     * @return the generated signature
     */
    public String signForBillingInfoUpdate(final String accountCode) {
        return sign(new HashMap<String, Object>() {{
            put(RecurlyConstants.JS_PARAM_ACCOUNT, new HashMap<String, Object>() {
                {
                    put(RecurlyConstants.JS_PARAM_ACCOUNT_CODE, accountCode);
                }
            });
        }});
    }

    /**
     * Convenience method for signing a typical one time transaction
     * @param accountCode the account code the transaction is for
     * @param amountInCents the transaction amount in cents
     * @return the generated signature
     */
    public String signOneTimeTransaction(final String accountCode, final int amountInCents) {
        if (accountCode == null || accountCode.isEmpty()) {
            throw new IllegalArgumentException("Account Code is required");
        }

        return sign(new HashMap<String, Object>() {{
            put(RecurlyConstants.JS_PARAM_ACCOUNT, new HashMap<String, Object>() {
                {
                    put(RecurlyConstants.JS_PARAM_ACCOUNT_CODE, accountCode);
                }
            });
            put(RecurlyConstants.JS_PARAM_TRANSACTION, new HashMap<String, Object>() {
                {
                    put(RecurlyConstants.JS_PARAM_AMOUNT_IN_CENTS, amountInCents);
                }
            });
        }});
    }
    
    /**
     * Create a signature using the private key and given map of arguments
     * @see <a href="http://docs.recurly.com/api/recurlyjs/signatures>Recurly.js Signature Generation</a>
     * @param args the map of arguments
     * @return the signature
     */
    public String sign(Map<String, Object> args) {
        HashMap<String, Object> newArgs = new HashMap<String, Object>(args);
        newArgs.put("timestamp", getTimestamp().getTime() / 1000);
        newArgs.put("nonce", getNonce());
        String query = Util.httpBuildQuery(newArgs, null);
        String signature = hash(getPrivateKey(), query) + '|' + query;
        if (log.isDebugEnabled()) {
            log.debug("Signature created: " + signature);
        }
        return signature;
    }

    /**
     * Hash a given message using a given private key.
     * @param privateKey the private key to be used
     * @param message the message to be hashed
     * @return the hash
     */
    protected String hash(String privateKey, String message) {
        if (message == null) {
            throw new IllegalArgumentException("Message is not set.");
        }

        if (privateKey == null || privateKey.length() != 32) {
            throw new IllegalArgumentException("Recurly private key is not set. The key must be 32 characters.");
        }

        try {
            Mac mac = Mac.getInstance(RecurlyConstants.HMAC_SHA1);
            SecretKeySpec secret = new SecretKeySpec(privateKey.getBytes(), RecurlyConstants.HMAC_SHA1);
            mac.init(secret);
            byte[] digest = mac.doFinal(message.getBytes());

            StringBuilder hash = new StringBuilder();
            for (byte b : digest) {
                hash.append(String.format("%02x", b));
            }
            return hash.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RecurlyJSException(e);
        } catch (InvalidKeyException e) {
            throw new RecurlyJSException(e);
        }
    }

    /**
     * Get the private key to be used when creating a signature.
     * @return the private key
     */
    private String getPrivateKey() {
        return keyProvider.getPrivateKey();
    }

    /**
     * Get a timestamp to be used when signing.
     * In its own function so it can be stubbed for testing.
     * @return the current Date
     */
    protected Date getTimestamp() {
        return new Date();
    }

    /**
     * Get a nonce to be used when signing.
     * In its own function so it can be stubbed for testing.
     * @return a random UUID string
     */
    protected String getNonce() {
        return UUID.randomUUID().toString();
    }
}
