package com.ast.back.modules.plagiarism.domain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

final class CStandardLibraryShimWriter {

    private CStandardLibraryShimWriter() {
    }

    static void writeTo(Path includeDir) {
        try {
            resetDirectory(includeDir);
            for (Map.Entry<String, String> entry : headers().entrySet()) {
                Path filePath = includeDir.resolve(entry.getKey()).normalize();
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, entry.getValue(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to prepare bundled C header shims", e);
        }
    }

    private static Map<String, String> headers() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("stdio.h", """
                #ifndef __AST_STDIO_H__
                #define __AST_STDIO_H__
                #include <stddef.h>
                typedef struct __ast_FILE FILE;
                extern FILE *stdin;
                extern FILE *stdout;
                extern FILE *stderr;
                int printf(const char *format, ...);
                int scanf(const char *format, ...);
                int fprintf(FILE *stream, const char *format, ...);
                int fscanf(FILE *stream, const char *format, ...);
                int sprintf(char *buffer, const char *format, ...);
                int snprintf(char *buffer, size_t size, const char *format, ...);
                int puts(const char *s);
                int getchar(void);
                int putchar(int c);
                char *fgets(char *buffer, int size, FILE *stream);
                FILE *fopen(const char *filename, const char *mode);
                int fclose(FILE *stream);
                size_t fread(void *ptr, size_t size, size_t count, FILE *stream);
                size_t fwrite(const void *ptr, size_t size, size_t count, FILE *stream);
                int feof(FILE *stream);
                int fflush(FILE *stream);
                #endif
                """);
        headers.put("stdlib.h", """
                #ifndef __AST_STDLIB_H__
                #define __AST_STDLIB_H__
                #include <stddef.h>
                int atoi(const char *str);
                long atol(const char *str);
                double atof(const char *str);
                void *malloc(size_t size);
                void *calloc(size_t count, size_t size);
                void *realloc(void *ptr, size_t size);
                void free(void *ptr);
                void exit(int status);
                int rand(void);
                void srand(unsigned int seed);
                int abs(int value);
                long labs(long value);
                #endif
                """);
        headers.put("string.h", """
                #ifndef __AST_STRING_H__
                #define __AST_STRING_H__
                #include <stddef.h>
                size_t strlen(const char *str);
                int strcmp(const char *left, const char *right);
                int strncmp(const char *left, const char *right, size_t count);
                char *strcpy(char *dest, const char *src);
                char *strncpy(char *dest, const char *src, size_t count);
                char *strcat(char *dest, const char *src);
                char *strncat(char *dest, const char *src, size_t count);
                char *strchr(const char *str, int ch);
                void *memcpy(void *dest, const void *src, size_t count);
                void *memmove(void *dest, const void *src, size_t count);
                void *memset(void *dest, int ch, size_t count);
                #endif
                """);
        headers.put("math.h", """
                #ifndef __AST_MATH_H__
                #define __AST_MATH_H__
                double sqrt(double value);
                double pow(double x, double y);
                double fabs(double value);
                double sin(double value);
                double cos(double value);
                double tan(double value);
                double log(double value);
                double exp(double value);
                #endif
                """);
        headers.put("ctype.h", """
                #ifndef __AST_CTYPE_H__
                #define __AST_CTYPE_H__
                int isalpha(int ch);
                int isdigit(int ch);
                int isalnum(int ch);
                int isspace(int ch);
                int toupper(int ch);
                int tolower(int ch);
                #endif
                """);
        headers.put("time.h", """
                #ifndef __AST_TIME_H__
                #define __AST_TIME_H__
                typedef long time_t;
                typedef long clock_t;
                time_t time(time_t *timer);
                clock_t clock(void);
                #endif
                """);
        headers.put("conio.h", """
                #ifndef __AST_CONIO_H__
                #define __AST_CONIO_H__
                int getch(void);
                int getche(void);
                #endif
                """);
        return headers;
    }

    private static void resetDirectory(Path includeDir) throws IOException {
        if (Files.exists(includeDir)) {
            try (var stream = Files.walk(includeDir)) {
                for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
        Files.createDirectories(includeDir);
    }
}
