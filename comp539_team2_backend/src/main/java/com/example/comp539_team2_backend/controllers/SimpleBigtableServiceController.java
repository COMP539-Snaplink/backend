//package com.example.comp539_team2_backend;
//
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//public class SimpleBigtableServiceController {
//    @GetMapping("/testBigtable")
//    public String testBigtable() {
//        try {
//            SimpleBigtableService bigtableService = new SimpleBigtableService();
//            return bigtableService.InsertandReadData();
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "Failure";
//        }
//    }
//}
