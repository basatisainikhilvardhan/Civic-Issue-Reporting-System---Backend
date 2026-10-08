
package com.example.Demo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
//@Controller
@RestController
@CrossOrigin(origins = "http://localhost:3000")

public class UserController  //localhost:8080/reg
{

    @Autowired
    UserRepository ur;

    @Autowired(required = false)
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @GetMapping("/d")
    String m1() 
    {
        return "Home";
    }
    @GetMapping("/reg")
     String m2() {
        return "register";
    }
    @PostMapping("/register")
    //String m3(String uname, String email, String psw, Model m)
    String m3(@RequestBody Users u)
    {
      if (u.getRole() == null) {
          u.setRole(Role.CITIZEN);
      }
      if (passwordEncoder != null && u.getPassword() != null && !u.getPassword().startsWith("$2a$") && !u.getPassword().startsWith("$2b$")) {
          u.setPassword(passwordEncoder.encode(u.getPassword()));
      }
      Users eu=this.ur.findByUsername(u.getUsername());
      if(eu!=null)
      {
        this.ur.save(u);
        return "registration successful";
      }
      else
      {
        return "invalid username";
      }

     }    


      @PostMapping("/login")
      ResponseEntity<?> m4(@RequestBody LoginData l)
      {
        Users eu=this.ur.findByUsername(l.uname);
        boolean passwordMatch = false;
        if (eu != null && eu.getPassword() != null && l.psw != null) {
            if (passwordEncoder != null && (eu.getPassword().startsWith("$2a$") || eu.getPassword().startsWith("$2b$"))) {
                passwordMatch = passwordEncoder.matches(l.psw, eu.getPassword());
            } else {
                passwordMatch = eu.getPassword().equals(l.psw);
            }
        }
        if(passwordMatch)
        {
            //return "login successful";
            return ResponseEntity.status(HttpStatus.OK).body(eu);
        }
        else
        {
            //return "invalid username or password";
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid username or password");
        }
      }

     @PostMapping("/delete")
     String m5(@RequestBody LoginData l)
     {
        Users eu=this.ur.findByUsername(l.uname);
            this.ur.delete(eu);
            return "user deleted successfully";

     }
}