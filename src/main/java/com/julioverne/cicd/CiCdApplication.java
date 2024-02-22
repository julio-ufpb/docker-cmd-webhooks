package com.julioverne.cicd;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Map;


/**
 * System listen for Webhook GitHub
 */
@RestController
@Service
@SpringBootApplication
public class CiCdApplication {
	private static final String VERSION = CiCdApplication.class.getPackage().getImplementationVersion();
	private static final String EOL = "\n";
	private static final int SIGNATURE_LENGTH = 45;
	private static final ObjectMapper mapper = new ObjectMapper();
	private final Environment env;

	public CiCdApplication(Environment env) {
		this.env = env;
	}

	public static void main(String[] args) {
		SpringApplication.run(CiCdApplication.class, args);
	}

	@RequestMapping(value = "/webhooks/{container}/{action}", method = RequestMethod.POST)
	public ResponseEntity<String> handle(@RequestHeader("X-Hub-Signature") String signature,
										 @RequestBody String payload,
										 @PathVariable("container") String container,
										 @PathVariable("action") String action) {

		HttpHeaders headers = new HttpHeaders();

		if (signature == null) {
			return new ResponseEntity<>(buildResponse("[-] No signature given."), headers, HttpStatus.BAD_REQUEST);
		}

		String computed = String.format("sha1=%s", new HmacUtils(HmacAlgorithms.HMAC_SHA_1, env.getProperty("PRIVATE_API_KEY")).hmacHex(payload));
		boolean invalidLength = signature.length() != SIGNATURE_LENGTH;

		if (invalidLength || !MessageDigest.isEqual(signature.getBytes(), computed.getBytes())) {
			return new ResponseEntity<>(buildResponse("[-] Invalid signature."), headers, HttpStatus.UNAUTHORIZED);
		}

		try {

			Map mapRequest = mapper.readValue(payload, Map.class);

			String baseBranch = (String)mapRequest.get("ref");
			String branchName = Paths.get(baseBranch).getFileName().toString();
			
			String repositoryName = (String)((Map)mapRequest.get("repository")).get("name");
			
			executeAction(repositoryName, branchName, container, action);

		} catch (Exception e) {
			return new ResponseEntity<>(buildResponse("[-] Failed Reload Service."), headers, HttpStatus.OK);
		}

		return new ResponseEntity<>(buildResponse("[*] OK: Service Reloaded."), headers, HttpStatus.OK);
	}

	private void executeAction(String repository, String branch, String container, String action) {
		try {
			String repositoryName = repository.replaceAll("[^a-zA-Z0-9._-]", "");
			String branchName = branch.replaceAll("[^a-zA-Z0-9._-]", "");
			String containerName = container.replaceAll("[^a-zA-Z0-9._-]", "");
			String actionName = action.replaceAll("[^a-zA-Z0-9._-]", "");

			System.out.println(buildResponse("[+] Handle Action for: repository[" + repositoryName + "], branch[" + branchName + "] ..."));

			ProcessBuilder processBuilder = new ProcessBuilder("/usr/local/bin/webhook-action.sh", repositoryName, branchName, containerName, actionName);
			Process process = processBuilder.start();
			int exitCode = process.waitFor();

			if (exitCode == 0) {
				System.out.println(buildResponse("[+] Successfully."));
			} else {
				System.out.println(buildResponse("[-] Failed. Exit code: " + exitCode));
			}
		} catch (Exception e) {
			System.out.println(buildResponse("[-] Failed for: repository[" + repository + "], branch[" + branch + "] ..."));
		}
	}

	private String buildResponse(String message) {
		return String.format("Docker-cmd-Webhooks v"+ VERSION +": %s" + EOL, message);
	}

}
