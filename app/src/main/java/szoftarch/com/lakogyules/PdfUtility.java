package szoftarch.com.lakogyules;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;


public class PdfUtility {
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void generatePDF(Map<String, String> usersWithShare, MainMenu mainMenu) {
        PdfDocument document = new PdfDocument();

        try{
            QRCodeWriter writer = new QRCodeWriter();
            int pageNum=1;
            for (String id : usersWithShare.keySet()) {
                String infoName = String.format("Név: %s", id);
                String infoShare = String.format("Tulajdonrész: %s", usersWithShare.get(id));

                PdfDocument.PageInfo.Builder pageBuilder = new PdfDocument.PageInfo.Builder(200,200, pageNum++);
                PdfDocument.Page page = document.startPage(pageBuilder.create());
                try {
                    String tmp = String.format("%s=%s", id, usersWithShare.get(id));
                    BitMatrix bitMatrix = writer.encode(tmp, BarcodeFormat.QR_CODE, 128, 128);
                    int width = bitMatrix.getWidth();
                    int height = bitMatrix.getHeight();
                    Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                        }
                    }
//                    LinearLayout l = (LinearLayout) findViewById(R.id.pdfLayout);
//                    TextView userInfoTextview = (TextView) findViewById(R.id.textUserInfo);
                    ImageView userQrImg = (ImageView) mainMenu.findViewById(R.id.imgQr);
                    userQrImg.setImageBitmap(bmp);
//                    userInfoTextview.setText(info);
                    Paint p = new Paint();
                    p.setColor(Color.BLACK);
                    page.getCanvas().drawText(infoName, 20, 25, p);
                    page.getCanvas().drawText(infoShare, 20, 40, p);
                    //userQrImg.draw(page.getCanvas());
                    page.getCanvas().drawBitmap(bmp, 20, 50, p);
//                    userInfoTextview.draw(page.getCanvas());
//                    l.draw(page.getCanvas());
                    document.finishPage(page);
                } catch (WriterException e) {
                    e.printStackTrace();
                }

            }


            File pdfDirPath = new File(mainMenu.getExternalCacheDir(), "pdfs");
            pdfDirPath.mkdirs();
            File file = new File(pdfDirPath, "shares.pdf");
            FileOutputStream os = new FileOutputStream(file);
            document.writeTo(os);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            if(document!=null){
                document.close();
            }
        }
    }
}
