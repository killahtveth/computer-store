package ru.ulstu.computerstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ulstu.computerstore.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}