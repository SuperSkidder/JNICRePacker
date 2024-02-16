import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LoadTest {


    public static void loadLibrary() throws IOException {
        File file = Files.createFile(Paths.get("123")).toFile();
    }
}
