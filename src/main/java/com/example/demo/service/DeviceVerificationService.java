package com.example.demo.service;

import com.example.demo.dto.MarkAttendanceRequest;
import com.example.demo.entity.Student;
import com.example.demo.exception.CustomException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class DeviceVerificationService {
    public void registerPublicKey(Student student, String base64PublicKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
            KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(keyBytes));
            student.setPublicKey(base64PublicKey);
        } catch (Exception e) {
            throw new CustomException("Invalid public key format");
        }
    }

    public void verifySignature(Student student, MarkAttendanceRequest request) {
        if (student.getPublicKey() == null) {
            return;
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(student.getPublicKey());
            PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(keyBytes));
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initVerify(publicKey);
            signature.update(request.getSignedPayload().getBytes(StandardCharsets.UTF_8));
            byte[] sigBytes = Base64.getDecoder().decode(request.getDeviceSignature());
            System.out.println("Stored public key: " + student.getPublicKey());
System.out.println("Public key length: " + student.getPublicKey().length());
            boolean valid = signature.verify(sigBytes);
            if (!valid) {
                throw new CustomException("Biometric signature verification failed");
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Signature verification error: " + e.getMessage());
        }
    }
}
