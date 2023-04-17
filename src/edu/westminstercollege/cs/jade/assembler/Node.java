package edu.westminstercollege.cs.jade.assembler;

import edu.westminstercollege.cs.jade.classfile.AccessFlag;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;
import java.util.Optional;

public sealed interface Node {

    ParserRuleContext ctx();

    record AsmFile(ParserRuleContext ctx, List<Directive> directives) implements Node {}

    sealed interface Directive extends Node { }

    record SourceDirective(ParserRuleContext ctx, String sourceFile) implements Directive {}
    record ClassDirective(ParserRuleContext ctx, Type type, List<AccessFlag> flags, String name) implements Directive {
        enum Type {
            Class, Interface, Enum, Annotation, Module
        }
    }

    record SuperDirective(ParserRuleContext ctx, String superclassName) implements Directive {}

    record ImplementsDirective(ParserRuleContext ctx, String interfaceName) implements Directive {}

    record FieldDirective(ParserRuleContext ctx, List<AccessFlag> flags, String name, String descriptor) implements Directive {}

    record MethodDirective(ParserRuleContext ctx, List<AccessFlag> flags, String name, String descriptor, Optional<Code> code) implements Directive {}

    record Code(ParserRuleContext ctx, List<CodeLine> lines) implements Node {}

    sealed interface CodeLine extends Node {}

    record LimitLocals(ParserRuleContext ctx, int locals) implements CodeLine {}
    record LimitStack(ParserRuleContext ctx, int stack) implements CodeLine {}

    record Instruction(ParserRuleContext ctx, Optional<String> label, String opcode, List<Operand> operands) implements CodeLine {}

    sealed interface Operand extends Node {
        record Int(ParserRuleContext ctx, String text) implements Operand {}
        record Long(ParserRuleContext ctx, String text) implements Operand {}
        record Double(ParserRuleContext ctx, String text) implements Operand {}
        record Float(ParserRuleContext ctx, String text) implements Operand {}
        record Word(ParserRuleContext ctx, String text) implements Operand {}
        record Str(ParserRuleContext ctx, String text) implements Operand {}
    }
}
