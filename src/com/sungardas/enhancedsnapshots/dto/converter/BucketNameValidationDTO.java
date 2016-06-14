package com.sungardas.enhancedsnapshots.dto.converter;


public class BucketNameValidationDTO {
    private boolean valid;
    private String message;

    public BucketNameValidationDTO(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public boolean isValid() {
        return valid;
    }
}
