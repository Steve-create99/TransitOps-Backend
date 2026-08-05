package com.transitops.backend.seed;

import com.transitops.backend.entity.*;
import com.transitops.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StopRepository stopRepository;
    private final RouteRepository routeRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final ScheduleRepository scheduleRepository;
    private final NotificationRepository notificationRepository;
    private final OrganizationSettingsRepository settingsRepository;
    private final TripMetricRepository tripMetricRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${transitops.bootstrap.admin-email}")
    private String adminEmail;

    @Value("${transitops.bootstrap.admin-password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (!userRepository.existsByEmail(adminEmail)) {
            userRepository.save(User.builder()
                    .firstName("System")
                    .lastName("Admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build());
        }

        // Ensure a demo DRIVER account exists and is linked for the mobile app
        ensureDriverUser();

        if (settingsRepository.count() == 0) {
            settingsRepository.save(OrganizationSettings.builder()
                    .organizationName("KNUST TransitOps")
                    .brandingPrimaryColor("#1D9E75")
                    .contactEmail(adminEmail)
                    .timezone("Africa/Accra")
                    .build());
        }

        if (stopRepository.count() > 0) {
            enrichDemoFleetIfNeeded();
            return;
        }

        Map<String, double[]> stops = Map.ofEntries(
                Map.entry("Tech Junction", new double[]{6.6698, -1.5765, 420, 0}),
                Map.entry("Main Gate", new double[]{6.6712, -1.5749, 180, 0}),
                Map.entry("Continental Roundabout", new double[]{6.6741, -1.5716, 510, 0}),
                Map.entry("Administration Block", new double[]{6.6758, -1.5710, 120, 0}),
                Map.entry("Prempeh II Library", new double[]{6.6769, -1.5703, 350, 0}),
                Map.entry("Great Hall", new double[]{6.6754, -1.5688, 280, 0}),
                Map.entry("University Hall / Katanga", new double[]{6.6762, -1.5672, 390, 0}),
                Map.entry("Unity Hall / Conti", new double[]{6.6755, -1.5730, 340, 0}),
                Map.entry("Africa Hall", new double[]{6.6763, -1.5742, 160, 0}),
                Map.entry("Queen Elizabeth II Hall", new double[]{6.6771, -1.5728, 210, 0}),
                Map.entry("Independence Hall", new double[]{6.6779, -1.5714, 290, 0}),
                Map.entry("Republic Hall", new double[]{6.6773, -1.5699, 310, 0}),
                Map.entry("College of Engineering", new double[]{6.6725, -1.5728, 480, 0}),
                Map.entry("College of Science", new double[]{6.6733, -1.5712, 410, 0}),
                Map.entry("Commercial Area / Market", new double[]{6.6760, -1.5670, 450, 0}),
                Map.entry("New Site Terminal", new double[]{6.6690, -1.5638, 380, 0})
        );

        String[] zones = {"Gateway", "Gateway", "General", "Academic", "Academic", "General", "Residential",
                "Residential", "Residential", "Residential", "Residential", "Residential",
                "Academic", "Academic", "Gateway", "Gateway"};
        int zi = 0;
        for (var e : stops.entrySet()) {
            double[] v = e.getValue();
            stopRepository.save(Stop.builder()
                    .name(e.getKey())
                    .zone(zones[Math.min(zi++, zones.length - 1)])
                    .latitude(v[0])
                    .longitude(v[1])
                    .averageRiders((int) v[2])
                    .wheelchairAccessible(true)
                    .status("Active")
                    .build());
        }

        RouteEntity r1 = routeRepository.save(RouteEntity.builder()
                .code("K-01").name("Tech Junction – Katanga Circular").color("#1D9E75")
                .startStop("Tech Junction").endStop("Continental Roundabout")
                .intermediateStops(List.of("Main Gate", "Administration Block", "Prempeh II Library", "Great Hall", "University Hall / Katanga"))
                .status("Active").frequencyMinutes(10).busCount(6).type("Circular").direction("Northbound")
                .build());
        RouteEntity r2 = routeRepository.save(RouteEntity.builder()
                .code("K-02").name("Halls Circular — North").color("#3B82F6")
                .startStop("Continental Roundabout").endStop("Continental Roundabout")
                .intermediateStops(List.of("Unity Hall / Conti", "Africa Hall", "Queen Elizabeth II Hall", "Independence Hall", "Republic Hall"))
                .status("Active").frequencyMinutes(12).busCount(5).type("Circular").direction("Northbound")
                .build());
        RouteEntity r3 = routeRepository.save(RouteEntity.builder()
                .code("K-03").name("Faculty Express").color("#F59E0B")
                .startStop("Tech Junction").endStop("Continental Roundabout")
                .intermediateStops(List.of("Main Gate", "College of Engineering", "College of Science"))
                .status("Active").frequencyMinutes(15).busCount(4).type("Express").direction("Eastbound")
                .build());
        RouteEntity r4 = routeRepository.save(RouteEntity.builder()
                .code("K-04").name("Commercial Shuttle").color("#8B5CF6")
                .startStop("Commercial Area / Market").endStop("Tech Junction")
                .intermediateStops(List.of("Continental Roundabout", "Main Gate"))
                .status("Active").frequencyMinutes(20).busCount(3).type("Shuttle").direction("Southbound")
                .build());
        RouteEntity r5 = routeRepository.save(RouteEntity.builder()
                .code("K-05").name("New Site Connector").color("#EC4899")
                .startStop("New Site Terminal").endStop("Tech Junction")
                .intermediateStops(List.of("College of Science", "Main Gate"))
                .status("Active").frequencyMinutes(25).busCount(2).type("Connector").direction("Westbound")
                .build());
        RouteEntity r6 = routeRepository.save(RouteEntity.builder()
                .code("K-06").name("Library – Stadium Loop").color("#EF4444")
                .startStop("Prempeh II Library").endStop("Prempeh II Library")
                .intermediateStops(List.of("Great Hall", "University Hall / Katanga", "Commercial Area / Market"))
                .status("Active").frequencyMinutes(18).busCount(3).type("Circular").direction("Clockwise")
                .build());

        User driverUser = userRepository.findByEmail("kwame.mensah@transitops.local").orElse(null);

        Driver d1 = driverRepository.save(Driver.builder()
                .firstName("Kwame").lastName("Mensah").phone("+233201111111")
                .email("kwame.mensah@transitops.local").licenseNumber("GH-DL-1001")
                .licenseExpiry(LocalDate.now().plusYears(2)).employmentStatus("ACTIVE")
                .availability("AVAILABLE").assignedRoute(r1).user(driverUser).build());
        Driver d2 = driverRepository.save(Driver.builder()
                .firstName("Ama").lastName("Osei").phone("+233202222222")
                .email("ama.osei@transitops.local").licenseNumber("GH-DL-1002")
                .licenseExpiry(LocalDate.now().plusYears(1)).employmentStatus("ACTIVE")
                .availability("ON_SHIFT").assignedRoute(r2).build());
        Driver d3 = driverRepository.save(Driver.builder()
                .firstName("Kofi").lastName("Asante").phone("+233203333333")
                .email("kofi.asante@transitops.local").licenseNumber("GH-DL-1003")
                .licenseExpiry(LocalDate.now().plusYears(3)).employmentStatus("ACTIVE")
                .availability("AVAILABLE").assignedRoute(r4).build());
        Driver d4 = driverRepository.save(Driver.builder()
                .firstName("Efua").lastName("Boateng").phone("+233204444444")
                .email("efua.boateng@transitops.local").licenseNumber("GH-DL-1004")
                .licenseExpiry(LocalDate.now().plusMonths(10)).employmentStatus("ACTIVE")
                .availability("AVAILABLE").assignedRoute(r5).build());

        Vehicle v1 = vehicleRepository.save(Vehicle.builder()
                .registrationNumber("GR-1001-22").make("Yutong").model("ZK6122")
                .capacity(45).status("ACTIVE").fuelLevel(78.0).gpsStatus("ONLINE")
                .latitude(6.6698).longitude(-1.5765).assignedRoute(r1).assignedDriver(d1)
                .maintenanceDue(LocalDate.now().plusMonths(2)).build());
        Vehicle v2 = vehicleRepository.save(Vehicle.builder()
                .registrationNumber("GR-1002-22").make("Toyota").model("Coaster")
                .capacity(30).status("ACTIVE").fuelLevel(62.0).gpsStatus("ONLINE")
                .latitude(6.6741).longitude(-1.5716).assignedRoute(r2).assignedDriver(d2)
                .maintenanceDue(LocalDate.now().plusMonths(1)).build());
        vehicleRepository.save(Vehicle.builder()
                .registrationNumber("GR-1003-22").make("Yutong").model("ZK6122")
                .capacity(45).status("AVAILABLE").fuelLevel(90.0).gpsStatus("ONLINE")
                .latitude(6.6712).longitude(-1.5749).assignedRoute(r3)
                .maintenanceDue(LocalDate.now().plusMonths(3)).build());
        Vehicle v4 = vehicleRepository.save(Vehicle.builder()
                .registrationNumber("GR-1004-22").make("Golden Dragon").model("XML6125")
                .capacity(50).status("ACTIVE").fuelLevel(55.0).gpsStatus("ONLINE")
                .latitude(6.6760).longitude(-1.5670).assignedRoute(r4).assignedDriver(d3)
                .maintenanceDue(LocalDate.now().plusWeeks(3)).build());
        vehicleRepository.save(Vehicle.builder()
                .registrationNumber("GR-1005-22").make("Toyota").model("Hiace")
                .capacity(18).status("ACTIVE").fuelLevel(71.0).gpsStatus("ONLINE")
                .latitude(6.6690).longitude(-1.5638).assignedRoute(r5).assignedDriver(d4)
                .maintenanceDue(LocalDate.now().plusMonths(2)).build());
        vehicleRepository.save(Vehicle.builder()
                .registrationNumber("GR-1006-22").make("Yutong").model("ZK6122")
                .capacity(45).status("MAINTENANCE").fuelLevel(40.0).gpsStatus("OFFLINE")
                .latitude(6.6754).longitude(-1.5688).assignedRoute(r6)
                .maintenanceDue(LocalDate.now().minusDays(2))
                .maintenanceNotes("Brake inspection in progress").build());

        maintenanceRecordRepository.save(MaintenanceRecord.builder()
                .vehicle(v1).serviceDate(LocalDate.now().minusDays(40))
                .description("Scheduled oil and filter service").cost(850.0).build());
        maintenanceRecordRepository.save(MaintenanceRecord.builder()
                .vehicle(v4).serviceDate(LocalDate.now().minusDays(12))
                .description("Tire rotation and alignment").cost(420.0).build());

        LocalDate today = LocalDate.now();
        scheduleRepository.save(Schedule.builder()
                .route(r1).serviceDate(today).departureTime(LocalTime.of(7, 0)).arrivalTime(LocalTime.of(7, 45))
                .weekdays(true).driver(d1).vehicle(v1).delayStatus("ON_TIME").status("SCHEDULED").build());
        scheduleRepository.save(Schedule.builder()
                .route(r2).serviceDate(today).departureTime(LocalTime.of(7, 15)).arrivalTime(LocalTime.of(8, 0))
                .weekdays(true).driver(d2).vehicle(v2).delayStatus("DELAYED").delayMinutes(8).status("IN_PROGRESS").build());
        scheduleRepository.save(Schedule.builder()
                .route(r3).serviceDate(today).departureTime(LocalTime.of(8, 0)).arrivalTime(LocalTime.of(8, 30))
                .weekdays(true).weekends(true).delayStatus("ON_TIME").status("SCHEDULED").build());

        User admin = userRepository.findByEmail(adminEmail).orElseThrow();
        notificationRepository.save(Notification.builder()
                .title("Welcome to TransitOps").message("Your operations portal is ready.")
                .category("SYSTEM").priority("LOW").user(admin).build());
        notificationRepository.save(Notification.builder()
                .title("Route K-02 delayed").message("Halls Circular is running 8 minutes behind.")
                .category("DELAY").priority("HIGH").user(admin).build());

        for (int i = 0; i < 7; i++) {
            LocalDate day = today.minusDays(i);
            for (RouteEntity route : List.of(r1, r2, r3, r4, r5, r6)) {
                tripMetricRepository.save(TripMetric.builder()
                        .route(route)
                        .metricDate(day)
                        .passengers(180 + (i * 12) + route.getBusCount() * 10)
                        .completedTrips(20 + route.getBusCount())
                        .delayedTrips(i % 3)
                        .averageSpeed(16.0 + (i % 4))
                        .onTimePercentage(88.0 + (i % 10))
                        .build());
            }
        }

        // Seed also creates notifications for the driver user
        if (driverUser != null) {
            notificationRepository.save(Notification.builder()
                    .title("Shift assigned — K-01")
                    .message("You are assigned to Tech Junction – Katanga Circular today.")
                    .category("SCHEDULE").priority("MEDIUM").user(driverUser).build());
        }
    }

    /** Adds missing KNUST demo routes/fleet on already-seeded databases without wiping data. */
    private void enrichDemoFleetIfNeeded() {
        if (routeRepository.existsByCode("K-04")) {
            return;
        }
        RouteEntity r4 = routeRepository.save(RouteEntity.builder()
                .code("K-04").name("Commercial Shuttle").color("#8B5CF6")
                .startStop("Commercial Area / Market").endStop("Tech Junction")
                .intermediateStops(List.of("Continental Roundabout", "Main Gate"))
                .status("Active").frequencyMinutes(20).busCount(3).type("Shuttle").direction("Southbound")
                .build());
        RouteEntity r5 = routeRepository.save(RouteEntity.builder()
                .code("K-05").name("New Site Connector").color("#EC4899")
                .startStop("New Site Terminal").endStop("Tech Junction")
                .intermediateStops(List.of("College of Science", "Main Gate"))
                .status("Active").frequencyMinutes(25).busCount(2).type("Connector").direction("Westbound")
                .build());
        RouteEntity r6 = routeRepository.save(RouteEntity.builder()
                .code("K-06").name("Library – Stadium Loop").color("#EF4444")
                .startStop("Prempeh II Library").endStop("Prempeh II Library")
                .intermediateStops(List.of("Great Hall", "University Hall / Katanga", "Commercial Area / Market"))
                .status("Active").frequencyMinutes(18).busCount(3).type("Circular").direction("Clockwise")
                .build());

        Driver d3 = driverRepository.save(Driver.builder()
                .firstName("Kofi").lastName("Asante").phone("+233203333333")
                .email("kofi.asante@transitops.local").licenseNumber("GH-DL-1003")
                .licenseExpiry(LocalDate.now().plusYears(3)).employmentStatus("ACTIVE")
                .availability("AVAILABLE").assignedRoute(r4).build());

        Vehicle v = vehicleRepository.save(Vehicle.builder()
                .registrationNumber("GR-1004-22").make("Golden Dragon").model("XML6125")
                .capacity(50).status("ACTIVE").fuelLevel(55.0).gpsStatus("ONLINE")
                .latitude(6.6760).longitude(-1.5670).assignedRoute(r4).assignedDriver(d3)
                .maintenanceDue(LocalDate.now().plusWeeks(3)).build());
        vehicleRepository.save(Vehicle.builder()
                .registrationNumber("GR-1005-22").make("Toyota").model("Hiace")
                .capacity(18).status("ACTIVE").fuelLevel(71.0).gpsStatus("ONLINE")
                .latitude(6.6690).longitude(-1.5638).assignedRoute(r5)
                .maintenanceDue(LocalDate.now().plusMonths(2)).build());
        vehicleRepository.save(Vehicle.builder()
                .registrationNumber("GR-1006-22").make("Yutong").model("ZK6122")
                .capacity(45).status("MAINTENANCE").fuelLevel(40.0).gpsStatus("OFFLINE")
                .latitude(6.6754).longitude(-1.5688).assignedRoute(r6)
                .maintenanceDue(LocalDate.now().minusDays(2))
                .maintenanceNotes("Brake inspection in progress").build());

        maintenanceRecordRepository.save(MaintenanceRecord.builder()
                .vehicle(v).serviceDate(LocalDate.now().minusDays(12))
                .description("Tire rotation and alignment").cost(420.0).build());

        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            LocalDate day = today.minusDays(i);
            for (RouteEntity route : List.of(r4, r5, r6)) {
                tripMetricRepository.save(TripMetric.builder()
                        .route(route)
                        .metricDate(day)
                        .passengers(150 + (i * 10) + route.getBusCount() * 8)
                        .completedTrips(15 + route.getBusCount())
                        .delayedTrips(i % 2)
                        .averageSpeed(15.0 + (i % 3))
                        .onTimePercentage(86.0 + (i % 8))
                        .build());
            }
        }
    }

    private void ensureDriverUser() {
        String email = "kwame.mensah@transitops.local";
        User driverUser = userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(User.builder()
                        .firstName("Kwame")
                        .lastName("Mensah")
                        .email(email)
                        .password(passwordEncoder.encode("Driver@12345"))
                        .role(Role.DRIVER)
                        .enabled(true)
                        .build()));

        driverRepository.findByUserId(driverUser.getId()).or(() ->
                driverRepository.findAll().stream()
                        .filter(d -> email.equalsIgnoreCase(d.getEmail()))
                        .findFirst()
        ).ifPresent(driver -> {
            if (driver.getUser() == null) {
                driver.setUser(driverUser);
                driverRepository.save(driver);
            }
        });
    }
}
