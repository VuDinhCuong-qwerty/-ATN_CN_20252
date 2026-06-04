package com.iam.identity.service.common;

import java.text.Normalizer;
import java.time.Year;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.iam.identity.repository.jpa.AuthUserProfileRepository;
import com.iam.identity.repository.jpa.AuthUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenDataService {

    private final AuthUserRepository userRepository;
    private final AuthUserProfileRepository userProfileRepository;

    public String genUsername(String fullName) {
        if (fullName == null || fullName.isBlank())
            return null;
        List<String> list = List.of(
                removeDiacritics(fullName)
                        .trim().replace("\\s+", " ")
                        .split("\\s+"));
        StringBuilder username = new StringBuilder(list.getLast());
        for (int i = 0; i < list.size() - 1; i++) {
            String item = list.get(i);
            if (item == null || item.isBlank())
                continue;
            username.append(item.charAt(0));
        }
        int count = userRepository.countUsername(username.toString().toUpperCase());
        username.append(count == 0 ? "" : String.valueOf(count));
        return username.toString().toUpperCase();
    }

    public String genPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+@&$";
        Random random = new Random();

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }

        return sb.toString();
    }

    public String[] splitFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return new String[] { "", "" };
        }
        fullName = fullName.trim().replaceAll("\\s+", " ").toUpperCase();

        String[] parts = fullName.split(" ");
        String lastName = parts[parts.length - 1];
        String firstName = String.join(" ",
                java.util.Arrays.copyOfRange(parts, 0, parts.length - 1));
        return new String[] { firstName, lastName };
    }

    public String removeDiacritics(String text) {
        if (text == null)
            return null;
        String normalized = Normalizer.normalize(text.trim(), Normalizer.Form.NFD);
        return normalized
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
    }

    public String getEmployeeCode() {
        String PREFIX = "BANK" + Year.now().toString();
        int count = userProfileRepository.countEmployeeCode(PREFIX);
        count++;
        StringBuilder employeeCode = new StringBuilder(PREFIX);
        employeeCode.append(String.format("%06d", count));
        return employeeCode.toString();
    }

}
