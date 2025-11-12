package ru.practicum.wallet;

import lombok.experimental.UtilityClass;
import org.springframework.stereotype.Component;
import ru.practicum.enums.OperationStatus;

@Component
public class ResponseFactory {
    public static OperationResponseDto createResponse(NewWalletOpsRequest request, OperationStatus operationStatus) {
        return OperationResponseDto.builder()
                .walletID(request.getWalletId())
                .amount(request.getAmount())
                .status(operationStatus)
                .build();
    }

    public static OperationResponseDto createDeniedResponse(NewWalletOpsRequest request) {
        return OperationResponseDto.builder()
                .walletID(request.getWalletId())
                .amount(request.getAmount())
                .status(OperationStatus.DENIED)
                .build();
    }
}
