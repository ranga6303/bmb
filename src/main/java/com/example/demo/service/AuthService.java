package com.example.demo.service;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.CompleteRegistrationRequest;
import com.example.demo.dto.InitiateRegistrationRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.MessageResponse;
import com.example.demo.entity.AuditLog;
import com.example.demo.entity.EmailVerificationToken;
import com.example.demo.entity.PasswordResetToken;
import com.example.demo.entity.Role;
import com.example.demo.entity.Student;
import com.example.demo.entity.Teacher;
import com.example.demo.entity.User;
import com.example.demo.entity.UserSession;
import com.example.demo.exception.CustomException;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.repository.EmailVerificationTokenRepository;
import com.example.demo.repository.PasswordResetTokenRepository;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.TeacherRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserSessionRepository;
import com.example.demo.security.JwtUtil;
import com.example.demo.security.TokenHashUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenHashUtil tokenHashUtil;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final UserSessionRepository userSessionRepository;
    private final AuditLogRepository auditLogRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public AuthService(
        AuthenticationManager authenticationManager,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtUtil jwtUtil,
        TokenHashUtil tokenHashUtil,
        EmailVerificationTokenRepository emailVerificationTokenRepository,
        PasswordResetTokenRepository passwordResetTokenRepository,
        EmailService emailService,
        UserSessionRepository userSessionRepository,
        AuditLogRepository auditLogRepository,
        StudentRepository studentRepository,
        TeacherRepository teacherRepository,
        org.springframework.transaction.PlatformTransactionManager transactionManager
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenHashUtil = tokenHashUtil;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.userSessionRepository = userSessionRepository;
        this.auditLogRepository = auditLogRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.transactionManager = transactionManager;
    }

    @Transactional
    public MessageResponse initiateRegistration(InitiateRegistrationRequest request) {
        String collegeId = request.getCollegeId().trim();

        Optional<Teacher> teacherOpt = teacherRepository.findByTeacherId(collegeId);
        Optional<Student> studentOpt = Optional.empty();
        if (teacherOpt.isEmpty()) {
            studentOpt = studentRepository.findByStudentId(collegeId);
        }

        if (teacherOpt.isEmpty() && studentOpt.isEmpty()) {
            throw new CustomException("College ID not found. Contact admin.");
        }

        User existingUser = null;
        if (teacherOpt.isPresent()) {
            existingUser = teacherOpt.get().getUser();
        } else if (studentOpt.isPresent()) {
            existingUser = studentOpt.get().getUser();
        }
        if (existingUser != null) {
            throw new CustomException("Account already exists");
        }

        String email = null;
        if (teacherOpt.isPresent()) {
            email = teacherOpt.get().getEmail();
        } else if (studentOpt.isPresent()) {
            email = studentOpt.get().getEmail();
        }
        if (email == null || email.isBlank()) {
            throw new CustomException("No email on record for this ID. Contact admin.");
        }

        emailVerificationTokenRepository.deleteByCollegeId(collegeId);

        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        String tokenHash = tokenHashUtil.hashToken(otp);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setTokenHash(tokenHash);
        token.setCollegeId(collegeId);
        token.setExpiryTime(LocalDateTime.now().plusMinutes(10));
        token.setUsed(false);
        token.setUser(null);
        emailVerificationTokenRepository.save(token);

        emailService.sendOtpEmail(email, otp);
        String maskedEmail = maskEmail(email);
        return new MessageResponse("OTP sent to " + maskedEmail + ". It expires in 10 minutes.");
    }

    @Transactional
    public MessageResponse completeRegistration(CompleteRegistrationRequest request) {
        String hashedOtp = tokenHashUtil.hashToken(request.getOtp());
        EmailVerificationToken token = emailVerificationTokenRepository.findByTokenHashAndUsedFalse(hashedOtp)
            .orElseThrow(() -> new IllegalStateException("Invalid or expired OTP."));

        if (token.getExpiryTime() != null && token.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("OTP has expired. Please request a new one.");
        }
        if (token.getCollegeId() == null || !token.getCollegeId().equals(request.getCollegeId())) {
            throw new IllegalStateException("Invalid or expired OTP.");
        }

        String collegeId = request.getCollegeId();
        Optional<Teacher> teacherOpt = teacherRepository.findByTeacherId(collegeId);
        Optional<Student> studentOpt = Optional.empty();
        if (teacherOpt.isEmpty()) {
            studentOpt = studentRepository.findByStudentId(collegeId);
        }

        if (teacherOpt.isEmpty() && studentOpt.isEmpty()) {
            throw new CustomException("College ID not found");
        }

        User existingUser = null;
        if (teacherOpt.isPresent()) {
            existingUser = teacherOpt.get().getUser();
        } else if (studentOpt.isPresent()) {
            existingUser = studentOpt.get().getUser();
        }
        if (existingUser != null) {
            throw new CustomException("Account already exists");
        }

        User user = new User();
        user.setUsername(collegeId);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setRegisteredDeviceId(null);

        if (teacherOpt.isPresent()) {
            Teacher teacher = teacherOpt.get();
            user.setRole(Role.SUBJECT_TEACHER);
            user.setEmail(teacher.getEmail());
            userRepository.save(user);
            teacher.setUser(user);
            teacherRepository.save(teacher);
        } else {
            Student student = studentOpt.get();
            user.setRole(Role.STUDENT);
            user.setEmail(student.getEmail());
            userRepository.save(user);
            student.setUser(user);
            studentRepository.save(student);
        }

        token.setUsed(true);
        emailVerificationTokenRepository.save(token);

        return new MessageResponse("Registration complete. You can now log in.");
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        return loginInternal(request, httpRequest, null);
    }

    @Transactional
    public AuthResponse loginStudent(LoginRequest request, HttpServletRequest httpRequest) {
        return loginInternal(request, httpRequest, true);
    }

    @Transactional
    public AuthResponse loginStaff(LoginRequest request, HttpServletRequest httpRequest) {
        return loginInternal(request, httpRequest, false);
    }

    private AuthResponse loginInternal(LoginRequest request, HttpServletRequest httpRequest, Boolean studentEndpoint) {
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new CustomException("Invalid username or password"));

        if (Boolean.TRUE.equals(studentEndpoint) && user.getRole() != Role.STUDENT) {
            throw new CustomException("This endpoint is only for student login.");
        }
        if (Boolean.FALSE.equals(studentEndpoint) && user.getRole() == Role.STUDENT) {
            throw new CustomException("Students must use /auth/login/student.");
        }

        if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Account is temporarily locked. Please try again later.");
        }

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            incrementFailed(user);
            throw new CustomException("Invalid username or password");
        }

        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        revokeAllSessions(user);

        String rawRefresh = tokenHashUtil.generateSecureToken();
        String refreshHash = tokenHashUtil.hashToken(rawRefresh);

        UserSession userSession = new UserSession();
        userSession.setUser(user);
        userSession.setDeviceId(user.getRegisteredDeviceId());
        userSession.setRefreshTokenHash(refreshHash);
        userSession.setIpAddress(httpRequest.getRemoteAddr());
        userSession.setUserAgent(httpRequest.getHeader("User-Agent"));
        userSession.setLastActiveAt(LocalDateTime.now());
        userSession.setRevoked(false);
        userSessionRepository.save(userSession);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
            .withUsername(user.getUsername())
            .password(user.getPassword())
            .authorities(new ArrayList<>())
            .build();

        String jwt = jwtUtil.generateToken(
            userDetails,
            user.getId(),
            user.getRole().name(),
            userSession.getId(),
            user.getRegisteredDeviceId()
        );

        persistAuditAfterCommit("LOGIN_SUCCESS", user, "User", user.getId(), "Login successful");
        return new AuthResponse(jwt, rawRefresh);
    }

    @Transactional
    public MessageResponse forgotPassword(String collegeId) {
        String trimmedId = collegeId.trim();

        Optional<Student> studentOpt = studentRepository.findByStudentId(trimmedId);
        Optional<Teacher> teacherOpt = teacherRepository.findByTeacherId(trimmedId);
        
        if (studentOpt.isEmpty() && teacherOpt.isEmpty()) {
            throw new CustomException("College ID not found. Please contact admin.");
        }

        User user = null;
        String email = null;
        
        if (studentOpt.isPresent()) {
            user = studentOpt.get().getUser();
            email = studentOpt.get().getEmail();
        } else {
            user = teacherOpt.get().getUser();
            email = teacherOpt.get().getEmail();
        }

        if (user == null) {
            throw new CustomException("No account exists for this ID. Please register first.");
        }

        if (email == null || email.isBlank()) {
            throw new CustomException("No email on record for this ID. Please contact admin.");
        }

        passwordResetTokenRepository.deleteByUser(user);

        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        String rawToken = collegeId + "::" + otp;
        String tokenHash = tokenHashUtil.hashToken(rawToken);

        PasswordResetToken token = new PasswordResetToken();
        token.setTokenHash(tokenHash);
        token.setUser(user);
        token.setExpiryTime(LocalDateTime.now().plusMinutes(15));
        token.setUsed(false);
        passwordResetTokenRepository.save(token);

        emailService.sendPasswordResetOtp(email, otp);
        
        String maskedEmail = maskEmail(email);
        return new MessageResponse("Password reset OTP sent to " + maskedEmail + ". It expires in 15 minutes.");
    }

    @Transactional
    public MessageResponse resetPassword(String rawToken, String newPassword) {
        String tokenHash = tokenHashUtil.hashToken(rawToken);
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new IllegalStateException("Invalid or expired reset token."));

        if (token.isUsed() || (token.getExpiryTime() != null && token.getExpiryTime().isBefore(LocalDateTime.now()))) {
            throw new IllegalStateException("Invalid or expired reset token.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLastPasswordChange(LocalDateTime.now());
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);
        passwordResetTokenRepository.deleteOtherTokensForUser(user.getId(), token.getId());

        return new MessageResponse("Password has been reset successfully.");
    }

    @Transactional
    public MessageResponse assignRole(Long userId, Role role) {
        if (role == null) {
            throw new CustomException("Role is required.");
        }
        if (role == Role.STUDENT) {
            throw new CustomException("Student role cannot be assigned from admin panel.");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException("User not found"));

        if (teacherRepository.findByUser(user).isEmpty()) {
            throw new CustomException("Only teacher accounts can be assigned staff roles.");
        }

        user.setRole(role);
        userRepository.save(user);

        persistAudit("ASSIGN_ROLE", user, "User", user.getId(), "Admin assigned role: " + role.name());
        return new MessageResponse("Role assigned successfully.");
    }

    @Transactional
    public MessageResponse resetDeviceBinding(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException("User not found"));
        user.setRegisteredDeviceId(null);
        userRepository.save(user);

        studentRepository.findByUser(user).ifPresent(student -> {
            student.setPublicKey(null);
            studentRepository.save(student);
        });

        revokeAllSessions(user);
        persistAudit("DEVICE_RESET", user, "User", user.getId(), "Device binding reset");
        return new MessageResponse("Device binding reset successfully.");
    }

    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken, HttpServletRequest request) {
        String hash = tokenHashUtil.hashToken(rawRefreshToken);
        UserSession oldSession = userSessionRepository.findByRefreshTokenHashAndRevokedFalse(hash)
            .orElseThrow(() -> new IllegalStateException("Invalid refresh token."));

        oldSession.setRevoked(true);
        userSessionRepository.save(oldSession);

        User user = oldSession.getUser();
        String newRawRefresh = tokenHashUtil.generateSecureToken();
        String newRefreshHash = tokenHashUtil.hashToken(newRawRefresh);

        UserSession newSession = new UserSession();
        newSession.setUser(user);
        newSession.setDeviceId(user.getRegisteredDeviceId());
        newSession.setRefreshTokenHash(newRefreshHash);
        newSession.setIpAddress(request.getRemoteAddr());
        newSession.setUserAgent(request.getHeader("User-Agent"));
        newSession.setLastActiveAt(LocalDateTime.now());
        newSession.setRevoked(false);
        userSessionRepository.save(newSession);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
            .withUsername(user.getUsername())
            .password(user.getPassword())
            .authorities(new ArrayList<>())
            .build();

        String accessToken = jwtUtil.generateToken(
            userDetails,
            user.getId(),
            user.getRole().name(),
            newSession.getId(),
            user.getRegisteredDeviceId()
        );

        return new AuthResponse(accessToken, newRawRefresh);
    }

    @Transactional
    public MessageResponse logout(String rawRefreshToken) {
        String hash = tokenHashUtil.hashToken(rawRefreshToken);
        userSessionRepository.findByRefreshTokenHashAndRevokedFalse(hash)
            .ifPresent(session -> {
                session.setRevoked(true);
                userSessionRepository.save(session);
            });
        return new MessageResponse("Logged out successfully");
    }

    private void revokeAllSessions(User user) {
        userSessionRepository.revokeAllActiveByUserId(user.getId());
    }

    private void incrementFailed(User user) {
        int failed = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(failed);
        if (failed >= 5) {
            user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(15));
        }
        userRepository.save(user);
    }

    private void resetFailed(User user) {
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
    }

    private void persistAudit(String action, User actor, String entity, Long targetId, String details) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setActorUser(actor);
        log.setTargetEntity(entity);
        log.setTargetId(targetId);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    private void persistAuditAfterCommit(String action, User actor, String entity, Long targetId, String details) {
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            persistAudit(action, actor, entity, targetId, details);
            return;
        }

        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    persistAuditInNewTransaction(action, actor, entity, targetId, details);
                }
            }
        );
    }

    private void persistAuditInNewTransaction(String action, User actor, String entity, Long targetId, String details) {
        org.springframework.transaction.support.TransactionTemplate transactionTemplate =
            new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(
            org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
        transactionTemplate.executeWithoutResult(status -> persistAudit(action, actor, entity, targetId, details));
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "***@" + domain;
        }
        
        return localPart.substring(0, 2) + "***@" + domain;
    }
}

