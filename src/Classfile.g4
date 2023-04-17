grammar Classfile;

@parser::header {
import java.nio.charset.StandardCharsets;
}

classfile
    : magic
        minor_version=u2
        major_version=u2
        constant_pool
        access_flags=u2
        this_class=u2
        super_class=u2
        interfaces
        fields
        /*
        constant_pool_count
        constant_pool
        access_flags
        this_class
        super_class
        interfaces_count
        interfaces
        fields_count
        fields
        methods_count
        methods
        attributes_count
        attributes
        */
        BYTE*
        EOF
    ;

magic
    : u4 {$u4.value == 0xCAFEBABE}?
    ;

constant_pool
locals [int count]
    : u2 { $count = $u2.value; }
        constant_pool_entries[$count - 1]
    ;

constant_pool_entries[int count]
    : ({$count > 0}? constant_pool_entry { $count--; })*
    /*: {$count == 1}? constant_pool_entry
    | {$count > 1}? constant_pool_entry constant_pool_entries[$count - 1]*/
    ;

constant_pool_entry
locals [int dataSize]
    : tag=u1 u2 {
        $dataSize = switch ($tag.value) {
            case 1 -> 2 + $u2.value;
            case 7, 8, 16, 19, 20 -> 2;
            case 15 -> 3;
            case 3, 4, 9, 10, 11, 12, 17, 18 -> 4;
            case 5, 6 -> 8;
            default ->
                throw new RuntimeException("Unknown constant pool entry tag: " + $tag.value + " at address " + $ctx.start.getStartIndex());
        } - 2; // subtracting 2 because of the u2 used for case 1
    }
    bytearray[$dataSize]
    ;

interfaces
locals [int count]
    : u2 { $count = $u2.value; }
      bytearray[2 * $count]
    ;

fields
locals [int count]
    : u2 { $count = $u2.value; }
    field_infos[$count]
    ;

field_infos[int count]
    : ({$count > 0}? field_info { $count--; })*
/*    : {$count == 0}?
    | {$count > 0}? field_info field_infos[$count - 1]*/
    ;

field_info
    : u2 u2 u2 attributes
    ;

attributes
locals [int count]
    : u2 { $count = $u2.value; }
    attribute_infos[$count]
    ;

attribute_infos[int count]
    : ({$count > 0}? attribute_info { $count--; })*
//    : {$count == 0}?
//    | {$count > 0}? attribute_info attribute_infos[$count - 1]
    ;

attribute_info
locals [int attribute_length]
    : u2 len=u4 {
        $attribute_length = $len.value;
    }
    bytearray[$attribute_length]
    ;

u1
returns [int value]
    : BYTE {
        $value = (int)$BYTE.text.charAt(0);
    }
    ;

u2
returns [int value]
    : b1=BYTE b2=BYTE {
        $value = ((int)$b1.text.charAt(0) << 8) | (int)$b2.text.charAt(0);
    }
    ;

u4
returns [int value]
    : b1=BYTE b2=BYTE b3=BYTE b4=BYTE {
        $value = ((int)$b1.text.charAt(0) << 24) | ((int)$b2.text.charAt(0) << 16) | ((int)$b3.text.charAt(0) << 8) | (int)$b4.text.charAt(0);
    }
    ;

u8
returns [long value]
    : high=u4 low=u4 {
        $value = ((long)$high.value << 32) | (long)$low.value;
    }
    ;

bytearray[int length]
    : ({$length > 0}? BYTE { $length--; })*
//    : {$length == 1}? BYTE
//    | {$length > 1}? BYTE bytearray[$length - 1]
    ;

BYTE
    : '\u0000' .. '\u00ff'
    ;
