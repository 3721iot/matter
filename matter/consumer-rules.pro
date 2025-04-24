# preserve the line number information for debugging stack traces.
-printmapping out.map
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# preserve chip classes
-keep class chip.** {*;}
-keepclassmembers  class chip.** {*;}

# preserve dsh classes
-keep class com.dsh.matter.** {*;}
-keepclassmembers  class com.dsh.** {*;}

# preserve chip native methods
-keepclasseswithmembernames,includedescriptorclasses class chip.** {
    native <methods>;
}

# preserve dsh native methods
-keepclasseswithmembernames,includedescriptorclasses class com.dsh.matter.** {
    native <methods>;
}