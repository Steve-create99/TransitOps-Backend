package com.transitops.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Service
public class EmailService {

    @Value("${transitops.resend.api-key:}")
    private String apiKey;

    @Value("${transitops.resend.from-email:TransitOps <onboarding@resend.dev>}")
    private String fromEmail;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public boolean send(String to, String subject, String html) {
        if (!isConfigured()) {
            log.warn("RESEND_API_KEY not set — skipping email to {}", to);
            return false;
        }
        try {
            String body = """
                    {"from":%s,"to":[%s],"subject":%s,"html":%s}
                    """.formatted(
                    jsonString(fromEmail),
                    jsonString(to),
                    jsonString(subject),
                    jsonString(html)
            ).trim();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Resend email accepted for {} (HTTP {})", to, response.statusCode());
                return true;
            }
            log.error("Resend email failed for {} — HTTP {}: {}", to, response.statusCode(), response.body());
            return false;
        } catch (Exception ex) {
            log.error("Resend email error for {}: {}", to, ex.getMessage());
            return false;
        }
    }

    public boolean sendDriverInvite(String to, String firstName, String acceptUrl, String expiresLabel) {
        String subject = "You're invited to KNUST TransitOps Driver Companion";
        String safeName = firstName != null && !firstName.isBlank() ? firstName : "Driver";
        String html = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#0f172a;font-family:Segoe UI,Roboto,Helvetica,Arial,sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#0f172a;padding:32px 16px;">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" style="max-width:560px;background:#1e293b;border-radius:16px;overflow:hidden;border:1px solid #334155;">
                        <tr>
                          <td style="background:linear-gradient(135deg,#0f766e,#1D9E75);padding:28px 32px;">
                            <p style="margin:0;color:#ecfdf5;font-size:12px;letter-spacing:0.12em;text-transform:uppercase;font-weight:700;">KNUST Campus</p>
                            <h1 style="margin:8px 0 0;color:#ffffff;font-size:22px;line-height:1.3;">TransitOps Driver Companion</h1>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:32px;">
                            <p style="margin:0 0 12px;color:#f1f5f9;font-size:16px;">Hello %s,</p>
                            <p style="margin:0 0 20px;color:#cbd5e1;font-size:14px;line-height:1.6;">
                              An administrator has invited you to join the <strong style="color:#f8fafc;">KNUST TransitOps</strong>
                              driver roster. Accept this invite to set your password and activate your DRIVER account
                              for the web portal and mobile Driver Companion app.
                            </p>
                            <p style="margin:0 0 28px;text-align:center;">
                              <a href="%s" style="display:inline-block;background:#1D9E75;color:#ffffff;text-decoration:none;
                                font-weight:700;font-size:14px;padding:14px 28px;border-radius:10px;">
                                Accept invitation
                              </a>
                            </p>
                            <p style="margin:0 0 8px;color:#94a3b8;font-size:12px;line-height:1.5;">
                              This link expires %s. If the button does not work, paste this URL into your browser:
                            </p>
                            <p style="margin:0;word-break:break-all;color:#64748b;font-size:11px;">%s</p>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:16px 32px 28px;border-top:1px solid #334155;">
                            <p style="margin:0;color:#64748b;font-size:11px;line-height:1.5;">
                              If you were not expecting this email, you can ignore it. Self-registration is disabled —
                              only invited drivers can activate an account.
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(escapeHtml(safeName), acceptUrl, escapeHtml(expiresLabel), acceptUrl);
        return send(to, subject, html);
    }

    private static String jsonString(String value) {
        if (value == null) return "null";
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }

    private static String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
