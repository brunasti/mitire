package it.brunasti.mitire.backend.config;

import it.brunasti.mitire.backend.domain.Group;
import it.brunasti.mitire.backend.domain.Project;
import it.brunasti.mitire.backend.domain.Role;
import it.brunasti.mitire.backend.domain.User;
import it.brunasti.mitire.backend.repository.GroupRepository;
import it.brunasti.mitire.backend.repository.ProjectRepository;
import it.brunasti.mitire.backend.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, ProjectRepository projectRepository,
                       GroupRepository groupRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.groupRepository = groupRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setFullName("Administrator");
            admin.setEmail("admin@mitire.local");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
        }

        if (projectRepository.count() == 0) {
            Project project = new Project();
            project.setCode("INTERNAL");
            project.setName("Internal / Admin");
            projectRepository.save(project);
        }

        if (groupRepository.count() == 0) {
            Group group = new Group();
            group.setName("Internal Team");
            group.setProjects(Set.copyOf(projectRepository.findAll()));
            groupRepository.save(group);

            if (userRepository.findByUsername("member").isEmpty()) {
                User member = new User();
                member.setUsername("member");
                member.setFullName("Sample Member");
                member.setEmail("member@mitire.local");
                member.setPasswordHash(passwordEncoder.encode("member123"));
                member.setRole(Role.MEMBER);
                member.setGroup(group);
                userRepository.save(member);
            }
        }
    }
}
