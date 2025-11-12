package ru.practicum.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ViolationResponseEntity {
    private final List<Violation> violations;
}
