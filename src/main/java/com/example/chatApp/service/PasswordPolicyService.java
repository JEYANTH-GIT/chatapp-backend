package com.example.chatApp.service;

import org.passay.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PasswordPolicyService {

    private final PasswordValidator validator;

    public PasswordPolicyService() {
        this.validator = new PasswordValidator(
                new LengthRule(8, 128),
                new CharacterRule(EnglishCharacterData.UpperCase, 1),
                new CharacterRule(EnglishCharacterData.LowerCase, 1),
                new CharacterRule(EnglishCharacterData.Digit, 1),
                new CharacterRule(EnglishCharacterData.Special, 1),
                new WhitespaceRule()
        );
    }

    public List<String> validate(String password) {
        RuleResult result = validator.validate(new PasswordData(password));
        if (result.isValid()) {
            return new ArrayList<>();
        }
        return validator.getMessages(result);
    }

    public boolean isValid(String password) {
        return validate(password).isEmpty();
    }
}
