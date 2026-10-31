package ru.ulstu.computerstore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.ulstu.computerstore.entity.User;
import ru.ulstu.computerstore.repository.UserRepository;
import ru.ulstu.sort.ParallelMergeSort;
import ru.ulstu.sort.SortResult;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSortService {

    private final UserRepository userRepository;

    public SortResult<User> getSortedUsers(String sortBy, int threads) {
        List<User> users = userRepository.findAll();
        Comparator<User> comparator = comparatorFor(sortBy);
        return ParallelMergeSort.sort(users, comparator, threads);
    }

    private Comparator<User> comparatorFor(String sortBy) {
        return switch (sortBy == null ? "id" : sortBy) {
            case "id"    -> Comparator.comparing(User::getId);
            case "name"  -> Comparator.comparing(User::getName);
            case "email" -> Comparator.comparing(User::getEmail);
            default -> throw new IllegalArgumentException("Неизвестное поле сортировки: " + sortBy);
        };
    }
}