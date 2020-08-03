package com.usststore.common.exception;

import com.usststore.common.enums.ExceptionEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UsException extends RuntimeException {

    private ExceptionEnum exceptionEnum;


}
