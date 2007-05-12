package ru.org.linux.site;

class SyntaxErrorException extends RuntimeException {
        public SyntaxErrorException(String info) { super(info); }
}
