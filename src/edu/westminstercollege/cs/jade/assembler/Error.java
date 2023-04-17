package edu.westminstercollege.cs.jade.assembler;

record Error(int line, String message, Severity severity) {

    enum Severity {
        Error, Warning
    }
}
