package com.pcc.document.util;

import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Magic-byte validation for the supported upload types. Checks the actual
 * stream signature rather than trusting Content-Type or the file extension.
 */
@Component
public class FileTypeValidator {

    public String detectMime(InputStream head) throws Exception {
        byte[] b = head.readNBytes(8);
        if (b.length < 4) return null;
        // PDF: %PDF
        if (b[0] == 0x25 && b[1] == 0x50 && b[2] == 0x44 && b[3] == 0x46)
            return "application/pdf";
        // JPEG: FF D8 FF
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF)
            return "image/jpeg";
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if ((b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A)
            return "image/png";
        // DOC/XLS (OLE2 compound): D0 CF 11 E0 A1 B1 1A E1 — ambiguous, caller
        // distinguishes via declared extension when needed
        if ((b[0] & 0xFF) == 0xD0 && (b[1] & 0xFF) == 0xCF && (b[2] & 0xFF) == 0x11
                && (b[3] & 0xFF) == 0xE0)
            return "application/octet-stream-ole2";
        // DOCX/XLSX (ZIP): PK\x03\x04 — also ambiguous, distinguished by extension
        if (b[0] == 0x50 && b[1] == 0x4B && b[2] == 0x03 && b[3] == 0x04)
            return "application/octet-stream-zip";
        return null;
    }
}
