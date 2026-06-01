package com.pedro.common.ai;

public enum Language {
     AUTO,
     EN,
     PT;

     public static Language parse(String value) {
          if (value == null || value.isBlank()) {
               return AUTO;
          }
          try {
               return Language.valueOf(value.toUpperCase());
          } catch (IllegalArgumentException ex) {
               return AUTO;
          }
     }
}
