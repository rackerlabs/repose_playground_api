package models;

import com.avaje.ebean.Model;
import helpers.EncryptionDecryptionAES;
import play.Play;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.persistence.*;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by dimi5963 on 11/28/15.
 */
@Entity
public class Cluster extends Model {
    @Id
    public Long id;

    @Column(length = 1496, nullable = false)
    public String capem;

    @Column(length = 1496, nullable = false)
    public String certpem;

    @Column(length = 2240, nullable = false)
    public String keypem;

    @Column(length = 2368, nullable = false)
    public String cakeypem;

    @Column(length = 1000, nullable = false)
    public String dockerenv;

    @Column(nullable = true)
    public String dockercmd;

    @Column(nullable = false)
    public Long user;

    public void setCapem(String capem) { this.capem = encrypt(capem); }
    public void setCertpem(String certpem) { this.certpem = encrypt(certpem); }
    public void setKeypem(String keypem) { this.keypem = encrypt(keypem); }
    public void setCakeypem(String cakeypem ) { this.cakeypem = encrypt(cakeypem); }
    public void setDockerenv(String dockerenv) { this.dockerenv = encrypt(dockerenv); }
    public void setDockercmd(String dockercmd) { this.dockercmd = encrypt(dockercmd); }
    public void setUser(Long user) { this.user = user; }

    public String getCapem() { return decrypt(this.capem); }
    public String getCertpem() { return decrypt(this.certpem); }
    public String getKeypem() { return decrypt(this.keypem); }
    public String getCakeypem() { return decrypt(this.cakeypem); }
    public String getDockerenv() { return decrypt(this.dockerenv); }
    public String getDockercmd() { return decrypt(this.dockercmd); }

    public static final Finder<Long, Cluster> find = new Finder<Long, Cluster>(
            Long.class, Cluster.class);

    public static Cluster findByUser(Long userId) {
        return find
                .where()
                .eq("user", userId)
                .findUnique();
    }

    public String encrypt(String value) {
        try {
            return EncryptionDecryptionAES.encrypt(value);
        }
        catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public String decrypt(String value) {
        try {
            return EncryptionDecryptionAES.decrypt(value);
        }
        catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }


    public String toString(){
        return "Cluster: " +
                "id => " + id +
                ", user => " + user;
    }


}
