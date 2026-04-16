package spring.springserver.domain.file.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import spring.springserver.global.exception.status_code.StatusCode;

@Getter
@RequiredArgsConstructor
public enum FileStatusCode implements StatusCode {
    FILE_EMPTY("FILE_EMPTY", "업로드할 파일이 비어 있습니다.", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_TYPE("INVALID_IMAGE_TYPE", "이미지 파일만 업로드할 수 있습니다.", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED("FILE_UPLOAD_FAILED", "파일 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
