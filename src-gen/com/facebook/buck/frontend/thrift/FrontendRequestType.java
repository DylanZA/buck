/**
 * Autogenerated by Thrift Compiler (0.12.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.facebook.buck.frontend.thrift;


@javax.annotation.Generated(value = "Autogenerated by Thrift Compiler (0.12.0)")
public enum FrontendRequestType implements org.apache.thrift.TEnum {
  UNKNOWN(0),
  LOG(5),
  ANNOUNCEMENT(13),
  FETCH_RULE_KEY_LOGS(22);

  private final int value;

  private FrontendRequestType(int value) {
    this.value = value;
  }

  /**
   * Get the integer value of this enum value, as defined in the Thrift IDL.
   */
  public int getValue() {
    return value;
  }

  /**
   * Find a the enum type by its integer value, as defined in the Thrift IDL.
   * @return null if the value is not found.
   */
  @org.apache.thrift.annotation.Nullable
  public static FrontendRequestType findByValue(int value) { 
    switch (value) {
      case 0:
        return UNKNOWN;
      case 5:
        return LOG;
      case 13:
        return ANNOUNCEMENT;
      case 22:
        return FETCH_RULE_KEY_LOGS;
      default:
        return null;
    }
  }
}
