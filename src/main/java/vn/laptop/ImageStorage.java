package vn.laptop;

import java.nio.file.*;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageStorage {
  @Value("${app.upload-dir}")
  String dir;

  public String save(MultipartFile file) {
    if (file == null || file.isEmpty()) return null;
    try {
      if (file.getSize() > 2 * 1024 * 1024) throw new IllegalArgumentException("Ảnh tối đa 2 MB");
      try (var in = ImageIO.createImageInputStream(file.getInputStream())) {
        var readers = ImageIO.getImageReaders(in);
        if (!readers.hasNext()) throw new IllegalArgumentException("Chỉ nhận ảnh JPEG hoặc PNG");
        var reader = readers.next();
        try {
          reader.setInput(in);
          String format = reader.getFormatName().toLowerCase();
          if (!java.util.Set.of("jpeg", "jpg", "png").contains(format))
            throw new IllegalArgumentException("Chỉ nhận JPEG/PNG");
          if (reader.getWidth(0) > 4000 || reader.getHeight(0) > 4000)
            throw new IllegalArgumentException("Kích thước ảnh tối đa 4000 × 4000");
          var image = reader.read(0);
          Path root = Path.of(dir).toAbsolutePath().normalize();
          Files.createDirectories(root);
          String name = java.util.UUID.randomUUID() + ".png";
          ImageIO.write(image, "png", root.resolve(name).toFile());
          return "/uploads/" + name;
        } finally {
          reader.dispose();
        }
      }
    } catch (java.io.IOException e) {
      throw new IllegalArgumentException("Không lưu được ảnh");
    }
  }
}
