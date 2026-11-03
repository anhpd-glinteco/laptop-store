package vn.laptop;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class Errors {
  @ExceptionHandler(IllegalArgumentException.class)
  public ModelAndView invalid(IllegalArgumentException e) {
    var m = new ModelAndView("problem");
    m.setStatus(HttpStatus.BAD_REQUEST);
    m.addObject("message", e.getMessage());
    return m;
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ModelAndView denied() {
    var m = new ModelAndView("problem");
    m.setStatus(HttpStatus.FORBIDDEN);
    m.addObject("message", "Bạn không có quyền truy cập nội dung này");
    return m;
  }

  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  public ModelAndView conflict() {
    var m = new ModelAndView("problem");
    m.setStatus(HttpStatus.CONFLICT);
    m.addObject("message", "Dữ liệu bị trùng hoặc đang được sử dụng. Hãy kiểm tra lại");
    return m;
  }
}
