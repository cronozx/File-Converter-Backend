package cronozx.fileconverterbackend.filehandling;

import com.convertapi.client.Config;
import com.convertapi.client.ConvertApi;
import com.convertapi.client.Param;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

@RestController
@CrossOrigin
@RequestMapping("/api")
public class FileConverterController {

    @PostMapping("/upload")
    public ResponseEntity<byte[]> convertFile(@RequestParam("file") MultipartFile file, @RequestParam("file-type") String fileType, @RequestParam("convert-type") String convertType) throws IOException {
        File tempFile = File.createTempFile("upload-", "." + fileType);
        Path fileLocation = Path.of(tempFile.getAbsolutePath());
        file.transferTo(tempFile);

        File convertedFile = File.createTempFile("converted-", "." + convertType);
        Path convertedFileLocation = Path.of(convertedFile.getAbsolutePath());

        try {
            Config.setDefaultSecret("*****");
            ConvertApi.convert(fileType, convertType,
                    new Param("File", fileLocation)
            ).get().saveFile(convertedFileLocation);
            Thread.sleep(3000);
        } catch (InterruptedException | ExecutionException e) {
            tempFile.deleteOnExit();
            convertedFile.deleteOnExit();
            throw new RuntimeException(e);
        }

        byte[] fileBytes = Files.readAllBytes(convertedFileLocation);
        final HttpHeaders httpHeaders = new HttpHeaders();

        try {
            httpHeaders.setContentDispositionFormData("attachments", convertedFile.getName());
            httpHeaders.setContentType(MediaType.valueOf(Files.probeContentType(convertedFile.toPath())));
            httpHeaders.setCacheControl("no-cache");
        } finally {
            tempFile.delete();
            convertedFile.delete();
        }

        return new ResponseEntity<>(fileBytes, httpHeaders, HttpStatus.OK);
    }
}
