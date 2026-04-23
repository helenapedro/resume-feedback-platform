package com.pedro.common.ai;

public enum Language {
     EN,
     PT;

     public static Language parse(String value) {
          if (value == null || value.isBlank()) {
               return EN;
          }
          try {
               return Language.valueOf(value.toUpperCase());
          } catch (IllegalArgumentException ex) {
               return EN;
          }
     }
}
