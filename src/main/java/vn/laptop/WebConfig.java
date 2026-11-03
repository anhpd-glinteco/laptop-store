package vn.laptop;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Value("${app.upload-dir}")
  String dir;

  public void addResourceHandlers(ResourceHandlerRegistry r) {
    r.addResourceHandler("/uploads/**")
        .addResourceLocations(Path.of(dir).toAbsolutePath().normalize().toUri().toString() + "/");
  }
}
