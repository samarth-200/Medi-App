package com.externship.appointment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import com.externship.appointment.Appointment_storage.AppointmentDelete;

import com.externship.appointment.Appointment_storage.Appointment;
import com.externship.appointment.Appointment_storage.AppointmentRepository;
import com.externship.appointment.Appointment_storage.AppointmentService;
import com.externship.appointment.Doctor_storage.Doctor;
import com.externship.appointment.Doctor_storage.DoctorRepository;
import com.externship.appointment.Person_storage.Person;
import com.externship.appointment.Person_storage.PersonRepository;




@Controller
public class ControllerClass {
	
	int count = 0;
	
	@Autowired
	private JavaMailSender mailSender;


	@Autowired
	PersonRepository personRepo;

	@Autowired
	DoctorRepository docRepo;

	@Autowired
	AppointmentRepository appRepo;
	
	 @Autowired
	 private AppointmentService appointmentService;
	  
	 

	@GetMapping("/register")
	public String register() {
		return "register";
	}

	@GetMapping("/registerdoc")
	public String registerdoc() {
		return "registerdoc";
	}

	@GetMapping("/")
	public String home() {
		return "start";
	}

	@GetMapping("/patlog")
	public String patlog() {
		return "index";
	}

	@GetMapping("/doclog")
	public String doclog() {
		return "doclog";
	}

	@PostMapping("/registered")
	public String registered(Person person) {
	    personRepo.save(person);
	    return "redirect:/";  // Redirect to the success_register page
	}
	@PostMapping("/registereddoc")
	public String registereddoc(Doctor doctor) {
	    try {
	        System.out.println("Received latitude: " + doctor.getLatitude());
	        System.out.println("Received longitude: " + doctor.getLongitude());

	        // Save the doctor details to the database
	        docRepo.save(doctor);

	        return "redirect:/";  // Redirect to the home page or a success page
	    } catch (Exception e) {
	        // Log the exception for debugging
	        e.printStackTrace();
	        return "redirect:/fail_register";  // Redirect to a page indicating registration failure
	    }
	}




	@GetMapping("/fail_login")
	public String fail_login() {
		return "fail_login";
	}

	@PostMapping("/authenticate")
	public String authenticate(Person person, HttpSession session) {
		if (personRepo.existsById(person.getEmail()) && personRepo.findById(person.getEmail()).get().getPassword().equals(person.getPassword())) {
			session.setAttribute("person", person.getEmail());
			return "redirect:/home";
		}
		return "redirect:/fail_login";
	}

	@PostMapping("/authenticatedoc")
	public String authenticatedoc(Doctor doctor, HttpSession session) {
		if (docRepo.existsById(doctor.getEmail()) && docRepo.findById(doctor.getEmail()).get().getPassword().equals(doctor.getPassword())) {
			session.setAttribute("doctor", doctor.getEmail());
			return "redirect:/doctor-home";
		}
		return "redirect:/fail_login";
	}

	@PostMapping("/cancel")
	public String cancel(AppointmentDelete dApp) {
		appRepo.deleteById(dApp.getAppId());
		return "redirect:/userdetails";
	}
	
	@PostMapping("/cancel1")
	public String cancel1(AppointmentDelete dApp) {
		appRepo.deleteById(dApp.getAppId());
		return "redirect:/patientlist";
	}

	@GetMapping("/home")
	public ModelAndView display(HttpSession session) {
		ModelAndView mav = new ModelAndView("fail_login");
		String email = null;

		if (session.getAttribute("person") != null) {
			mav = new ModelAndView("home");
			email = (String) session.getAttribute("person");
		}

		mav.addObject("email", email);

		return mav;
	}

	@PostMapping("/assignment")
	public String submitted(Appointment app) {
		app.setAppId(count++);
		app.setStatus("Active");
		appRepo.save(app);
		sendConfirmationEmail(app);
		
		return "redirect:/home";
	}
	
	
	public void sendConfirmationEmail(Appointment app) {
	 
		 SimpleMailMessage message = new SimpleMailMessage();
		    message.setFrom("koushikgdatta5@gmail.com"); // Replace with your email address
		    message.setTo(app.getEmail());
		    message.setSubject("Appointment Confirmation");
		    message.setText("Dear  Sir/Madam " + "\n\n" +
		                    "Your appointment with Dr. " + app.getDocName() + " has been confirmed. \n" +
		                    "Date: " + app.getDate() + "\n"  +
		                    "Specialty: " + app.getDocSpecial() + "\n\n" +
		                    "Thank you for booking with us. \n\n" +
		                    "Sincerely, \n" +
		                    "Doctor's Appointment System");
		    mailSender.send(message);
	
	}

	@GetMapping("/docdetails")
	public ModelAndView DocDetails(HttpSession session) {
		List<Doctor> doctors = new ArrayList<>();
		docRepo.findAll().forEach(doctors::add);
		Map<String, Object> params = new HashMap<>();
		params.put("doctor", doctors);
		params.put("email", session.getAttribute("person"));
		return new ModelAndView("doctorlist", params);
	}

	@GetMapping("/userdetails")
	public ModelAndView UserDetails(HttpSession session) {
		List<Appointment> apps = appRepo.findAllByEmail(session.getAttribute("person").toString());
		Map<String, Object> params = new HashMap<>();
		params.put("appointments", apps);
		params.put("email", session.getAttribute("person"));
		return new ModelAndView("appointed", params);
	}

	@GetMapping("/patientlist")
	public ModelAndView PatientList(HttpSession session) {
		List<Appointment> apps = appRepo.findByDocId(session.getAttribute("doctor").toString());
		Map<String, Object> params = new HashMap<>();
		params.put("appointments", apps);
		params.put("email", session.getAttribute("doctor"));
		return new ModelAndView("patientlist", params);
	}
	
	@ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception e) {
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("errorMessage", "An error occurred: " + e.getMessage());
        return mav;
    }
	
	@GetMapping("/doctor-home")
    public ModelAndView doctorHome(HttpSession session) {
        String email = (String) session.getAttribute("doctor");
        if (email != null) {
            return new ModelAndView("doctor-home", "email", email);
        } else {
            return new ModelAndView("redirect:/fail_login");
        }
    }
	
	@GetMapping("/booking")
	public String showBookingForm() {
		// Logic to populate any required data for the booking form
		return "booking"; // Assuming you have a Thymeleaf template named "bookingForm"
	}

	@GetMapping("/doctors")
	public String showDoctorForm() {
		// Logic to populate any required data for the booking form
		return "doctors"; // Assuming you have a Thymeleaf template named "bookingForm"
	}

	@GetMapping("/appointedDoc")
	public String showAppointedDoc() {
		// Logic to populate any required data for the booking form
		return "appointedDoc"; // Assuming you have a Thymeleaf template named "bookingForm"
	}
	
	 @GetMapping("/appointment-history")
	    public String showAppointmentHistory(Model model) {
	        List<Appointment> appointmentHistory = appointmentService.getAppointmentHistory();
	        model.addAttribute("appointments", appointmentHistory); // Update the attribute name
	        return "appointment-history";
	    }

	/*
	 * @PostMapping("/doclog") public String handleRegistration(@ModelAttribute
	 * Doctor doctor) { System.out.println("Received latitude: " +
	 * doctor.getLatitude()); System.out.println("Received longitude: " +
	 * doctor.getLongitude());
	 * 
	 * // Your existing code to save the doctor in the database
	 * docRepo.save(doctor);
	 * 
	 * return "redirect:/success-page"; // Redirect to a success page after
	 * processing }
	 */
	 
	
}