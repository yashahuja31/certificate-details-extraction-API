package com.example.certificates.service;

import com.example.certificates.model.CertificateDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CertificateFieldParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateFieldParser.class);
    private static final Pattern CERT_NUMBER = Pattern.compile("(?i)^(?:certificate\\s*(?:number|no\\.?|id)|certificate\\s*#|credential\\s*(?:id|number)|cert(?:ificate)?\\s*(?:no\\.?|#))\\s*[:#\\-]?\\s*(.*)$");
    private static final Pattern HOLDER = Pattern.compile("(?i)^(?:certificate\\s*holder|holder\\s*name|candidate\\s*name|recipient|awarded\\s*to|presented\\s*to|this\\s+is\\s+to\\s+certify\\s+that|certifies\\s+that|name)\\s*[:\\-]?\\s*(.*)$");
    private static final Pattern CERT_NAME = Pattern.compile("(?i)^(?:certificate\\s*name|certificate\\s*title|certificate\\s*type|course\\s*name|course\\s*title|training\\s*program|certification\\s*title|title)\\s*[:\\-]?\\s*(.*)$");
    private static final Pattern ISSUER = Pattern.compile("(?i)^(?:issuing\\s+organization|issuing\\s+organisation|issuing\\s+authority|issued\\s+by|issuer|organization|organisation|institution|awarded\\s+by|provided\\s+by|certified\\s+by)\\s*[:\\-]?\\s*(.*)$");
    private static final Pattern ISSUE_DATE = Pattern.compile("(?i)^(?:issue\\s+date|issued\\s+on|date\\s+of\\s+issue|completion\\s+date|date\\s+awarded)\\s*[:\\-]?\\s*(.*)$");
    private static final Pattern EXPIRY_DATE = Pattern.compile("(?i)^(?:expiry\\s+date|expiration\\s+date|expires\\s+on|valid\\s+until|valid\\s+through|valid\\s+thru|validity\\s+until|date\\s+of\\s+expiry|expiration|expiry)\\s*[:\\-]?\\s*(.*)$");
    // Used to stop one extracted field before the next field in OCR text where all text may be one line.
    private static final Pattern NEXT_FIELD_LABEL = Pattern.compile(
            "(?i)(?:certificate\\s*(?:number|no\\.?|id|#)|credential\\s*(?:id|number)|cert(?:ificate)?\\s*(?:no\\.?|#)|"
            + "certificate\\s*(?:holder|name|title|type)|holder\\s*name|candidate\\s*name|recipient|awarded\\s*to|presented\\s*to|"
            + "this\\s+is\\s+to\\s+certify\\s+that|certifies\\s+that|name|course\\s*(?:name|title)|training\\s*program|certification\\s*title|title|"
            + "issuing\\s+(?:organization|organisation|authority)|issued\\s*by|issuer|organization|organisation|institution|awarded\\s*by|provided\\s*by|certified\\s*by|"
            + "issue\\s*date|issued\\s*on|date\\s*of\\s*issue|completion\\s*date|date\\s*awarded|"
            + "expiry\\s*date|expiration\\s*date|expires\\s*on|valid\\s*(?:until|through|thru)|validity\\s*until|date\\s*of\\s*expiry|expiration|expiry)\\s*[:：#\\-]");
    private static final Pattern DATE_TOKEN = Pattern.compile("(?i)(\\b\\d{4}-\\d{1,2}-\\d{1,2}\\b|\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{4}\\b|\\b\\d{1,2}\\s+(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\s+\\d{4}\\b|\\b(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\s+\\d{1,2},?\\s+\\d{4}\\b)");
    private static final List<DateTimeFormatter> WORD_DATE_FORMATS = List.of(
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMMM uuuu").toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMM uuuu").toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMMM d uuuu").toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMM d uuuu").toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMM d, uuuu").toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MMMM d, uuuu").toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT)
    );

    private final Clock clock;

    public CertificateFieldParser(Clock clock) {
        this.clock = clock;
    }

    public CertificateFieldParser() {
        this(Clock.systemUTC());
    }

    public CertificateDetails parse(String text) {
        LOGGER.debug("Parsing extracted certificate text; characterCount={}", text == null ? 0 : text.length());
        if (text == null || text.isBlank()) {
            LOGGER.warn("Certificate field parser received empty text");
            return new CertificateDetails(null, null, null, null, null, null, "unknown");
        }
        String number = clean(findLabeledValue(text, CERT_NUMBER));
        String holder = clean(findLabeledValue(text, HOLDER));
        String certificateName = clean(findLabeledValue(text, CERT_NAME));
        String issuer = clean(findLabeledValue(text, ISSUER));
        String issueDate = parseDate(findLabeledValue(text, ISSUE_DATE));
        String expiryDate = parseDate(findLabeledValue(text, EXPIRY_DATE));
        if (certificateName == null) certificateName = inferCertificateName(text);

        String status = "unknown";
        if (expiryDate != null) {
            LocalDate expiry = LocalDate.parse(expiryDate);
            status = expiry.isBefore(LocalDate.now(clock)) ? "expired" : "valid";
        } else if (isExplicitlyNonExpiring(text)) {
            // Only honor explicit wording; a missing date by itself is not proof of lifetime validity.
            status = "valid";
        }
        int populatedFields = 0;
        if (number != null) populatedFields++;
        if (holder != null) populatedFields++;
        if (certificateName != null) populatedFields++;
        if (issuer != null) populatedFields++;
        if (issueDate != null) populatedFields++;
        if (expiryDate != null) populatedFields++;
        LOGGER.debug("Certificate field parsing completed; populatedFields={}, status={}", populatedFields, status);
        return new CertificateDetails(number, holder, certificateName, issuer, issueDate, expiryDate, status);
    }

    private String findLabeledValue(String text, Pattern labelPattern) {
        String[] lines = text.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            Matcher lineMatcher = labelPattern.matcher(lines[i].trim());
            if (!lineMatcher.matches()) continue;
            String value = lineMatcher.group(1) == null ? "" : lineMatcher.group(1).trim();
            if (value.isBlank()) {
                // Some PDF text extractors insert blank lines between a label and its value.
                for (int nextIndex = i + 1; nextIndex < lines.length; nextIndex++) {
                    String nextLine = lines[nextIndex].trim();
                    if (nextLine.isBlank()) continue;
                    if (!NEXT_FIELD_LABEL.matcher(nextLine).find()) value = nextLine;
                    break;
                }
            }
            return trimAtNextFieldLabel(value);
        }

        // OCR/PDF libraries sometimes return all fields in one text run. Search for the label
        // anywhere, then stop its value when the next recognized field label appears.
        String labelOnlyPattern = labelPattern.pattern()
                .replaceFirst("^\\(\\?i\\)\\^", "(?i)")
                .replaceFirst("\\(\\.\\*\\)\\$$", "");
        Pattern searchPattern = Pattern.compile(labelOnlyPattern, Pattern.CASE_INSENSITIVE);
        String flattened = text.replaceAll("[\\r\\n]+", " ").replaceAll("\\s{2,}", " ").trim();
        Matcher matcher = searchPattern.matcher(flattened);
        while (matcher.find()) {
            if (labelPattern == HOLDER) {
                String prefix = flattened.substring(0, matcher.start());
                // The generic holder alias "name" must not match inside "Certificate Name" or "Course Name".
                if (prefix.matches("(?is).*\\b(?:certificate|course)\\s+$")) continue;
            }
            return trimAtNextFieldLabel(flattened.substring(matcher.end()));
        }
        return null;
    }

    private String trimAtNextFieldLabel(String value) {
        Matcher next = NEXT_FIELD_LABEL.matcher(value);
        if (next.find()) value = value.substring(0, next.start());
        value = value.replaceFirst("^[\\s:：#\\-]+", "").trim();
        return value.isBlank() ? null : value;
    }

    private String clean(String value) {
        if (value == null) return null;
        String cleaned = value.replaceAll("\\s{2,}", " ").trim();
        cleaned = cleaned.replaceAll("[|]+$", "").trim();
        if (cleaned.isBlank() || cleaned.matches("(?i)^(n/?a|none|null|unknown|not applicable)$")) return null;
        // Avoid returning a full paragraph when OCR failed to preserve line breaks.
        return cleaned.length() > 120 ? cleaned.substring(0, 120).trim() : cleaned;
    }

    private boolean isExplicitlyNonExpiring(String text) {
        return Pattern.compile("(?i)\\b(?:this certificate\\s+(?:does not|will not|never)\\s+expire|non[- ]expiring certificate|permanent certificate|valid for (?:life|a lifetime)|lifetime certificate)\\b")
                .matcher(text).find();
    }

    private String inferCertificateName(String text) {
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.trim().replaceAll("\\s{2,}", " ");
            if (line.length() < 9 || line.length() > 100) continue;
            if (line.matches("(?i).*\\b(certificate of (completion|achievement|participation|training|appreciation)|completion certificate|professional certificate|training certificate)\\b.*")) {
                if (!line.matches("(?i).*(number|serial|expiry|expiration|issue date|issued by|valid until).*")) return clean(line);
            }
        }
        return null;
    }

    private String parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        Matcher matcher = DATE_TOKEN.matcher(value);
        if (!matcher.find()) return null;
        String token = matcher.group(1).replaceAll("\\s+", " ").replaceAll(",(?=\\d)", ", ").trim();
        try {
            if (token.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
                DateTimeFormatter iso = new DateTimeFormatterBuilder().appendPattern("uuuu-M-d").toFormatter().withResolverStyle(ResolverStyle.STRICT);
                return LocalDate.parse(token, iso).toString();
            }
            if (token.matches("\\d{1,2}[/-]\\d{1,2}[/-]\\d{4}")) {
                String[] pieces = token.split("[/-]");
                int first = Integer.parseInt(pieces[0]);
                int second = Integer.parseInt(pieces[1]);
                // Do not guess whether 03/04/2027 means 3 April or March 4.
                if (first <= 12 && second <= 12 && first != second) return null;
                int day, month;
                if (first > 12) { day = first; month = second; }
                else if (second > 12) { month = first; day = second; }
                else { day = first; month = second; }
                return LocalDate.of(Integer.parseInt(pieces[2]), month, day).toString();
            }
            for (DateTimeFormatter format : WORD_DATE_FORMATS) {
                try { return LocalDate.parse(token, format).toString(); }
                catch (DateTimeParseException ignored) { /* Try the next format. */ }
            }
        } catch (java.time.DateTimeException | IllegalArgumentException ignored) {
            return null;
        }
        return null;
    }
}
