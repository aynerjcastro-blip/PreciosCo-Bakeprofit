package com.comprasco.bakeprofit.exception;

public class InactiveUserException extends  RuntimeException{
        public InactiveUserException (String message){
            super(message);
        }
}
