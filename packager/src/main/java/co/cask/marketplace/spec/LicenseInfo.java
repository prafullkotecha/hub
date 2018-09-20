package co.cask.marketplace.spec;

/**
 * Information about a license.
 */
public class LicenseInfo {
  private final String name;
  private final String url;

  public LicenseInfo(String name, String url) {
    this.name = name;
    this.url = url;
  }
}
