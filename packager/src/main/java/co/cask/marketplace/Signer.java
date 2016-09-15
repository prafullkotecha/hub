/*
 * Copyright © 2016 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.marketplace;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.SignatureException;

/**
 * Creates detached signatures.
 */
public class Signer {

  private final PGPSignatureGenerator signer;

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public static Signer fromKeyFile(File keyFile, String keyPassword) throws IOException, PGPException,
    NoSuchProviderException, NoSuchAlgorithmException {
    try (InputStream is = PGPUtil.getDecoderStream(new FileInputStream(keyFile))) {
      PGPSecretKeyRingCollection secretKeyRing = new PGPSecretKeyRingCollection(is);
      PGPSecretKeyRing pgpSecretKeyRing = (PGPSecretKeyRing) secretKeyRing.getKeyRings().next();
      PGPSecretKey secretKey = pgpSecretKeyRing.getSecretKey();
      PGPPrivateKey privateKey = secretKey.extractPrivateKey(keyPassword.toCharArray(), "BC");
      int algo = secretKey.getPublicKey().getAlgorithm();

      PGPSignatureGenerator signer = new PGPSignatureGenerator(algo, PGPUtil.SHA1, "BC");
      signer.initSign(PGPSignature.BINARY_DOCUMENT, privateKey);
      return new Signer(signer);
    }
  }

  private Signer(PGPSignatureGenerator signer) {
    this.signer = signer;
  }

  public File signFile(File fileToSign) throws IOException, SignatureException, NoSuchAlgorithmException,
    NoSuchProviderException, PGPException {

    File sigFile = new File(fileToSign.getParentFile(), fileToSign.getName() + ".asc");

    // logic comes from DetachedSignatureProcessor example from bouncy castle
    byte[] buffer = new byte[1024 * 1024];
    try (BufferedInputStream is  = new BufferedInputStream(new FileInputStream(fileToSign))) {
      int len;
      while (is.available() != 0) {
        len = is.read(buffer);
        signer.update(buffer, 0, len);
      }
    }

    try (BCPGOutputStream bOut = new BCPGOutputStream(new ArmoredOutputStream(new FileOutputStream(sigFile)))) {
      signer.generate().encode(bOut);
    }
    return sigFile;
  }
}
