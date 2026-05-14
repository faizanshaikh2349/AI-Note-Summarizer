package com.example.ainotessummarizer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;

import java.io.InputStream;
import java.util.List;

public final class AttachmentTextExtractor {
    private static final int MAX_PDF_PAGES = 12;

    private AttachmentTextExtractor() {
    }

    public static String extractImageText(Context context, Uri uri) throws Exception {
        InputImage inputImage = InputImage.fromFilePath(context, uri);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        try {
            return Tasks.await(recognizer.process(inputImage)).getText();
        } finally {
            recognizer.close();
        }
    }

    public static String extractPdfText(Context context, Uri uri) throws Exception {
        StringBuilder builder = new StringBuilder();
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        try (ParcelFileDescriptor fileDescriptor =
                     context.getContentResolver().openFileDescriptor(uri, "r");
             PdfRenderer renderer = new PdfRenderer(fileDescriptor)) {

            int pageCount = Math.min(renderer.getPageCount(), MAX_PDF_PAGES);
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                try (PdfRenderer.Page page = renderer.openPage(pageIndex)) {
                    Bitmap bitmap = Bitmap.createBitmap(
                            page.getWidth() * 2,
                            page.getHeight() * 2,
                            Bitmap.Config.ARGB_8888);
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
                    String text = Tasks.await(recognizer.process(inputImage)).getText();
                    if (text != null && !text.trim().isEmpty()) {
                        builder.append(text).append("\n\n");
                    }
                    bitmap.recycle();
                }
            }
        } finally {
            recognizer.close();
        }

        return builder.toString().trim();
    }

    public static String extractDocxText(Context context, Uri uri) throws Exception {
        StringBuilder builder = new StringBuilder();

        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             XWPFDocument document = new XWPFDocument(inputStream)) {

            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    builder.append(text.trim()).append("\n");
                }
            }

            for (XWPFTable table : document.getTables()) {
                String tableText = table.getText();
                if (tableText != null && !tableText.trim().isEmpty()) {
                    builder.append(tableText.trim()).append("\n");
                }
            }
        }

        return builder.toString().trim();
    }
}
