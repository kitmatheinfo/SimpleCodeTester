package me.ialistannen.simplecodetester.checks;

import static java.util.stream.Collectors.toList;

import edu.kit.informatik.Terminal;
import java.util.ArrayList;
import java.util.List;
import me.ialistannen.simplecodetester.checks.ImmutableSubmissionCheckResult.Builder;
import me.ialistannen.simplecodetester.submission.CompiledFile;
import me.ialistannen.simplecodetester.submission.CompiledSubmission;
import me.ialistannen.simplecodetester.util.ErrorLogCapture;

/**
 * A class that runs a given suit of tests against a {@link CompiledSubmission}, reporting the
 * output.
 */
public class CheckRunner {

  private List<Check> checks;

  /**
   * Creates a new CheckRunner that will check a {@link CompiledSubmission}.
   *
   * @param checks the {@link Check}s to use
   */
  public CheckRunner(List<Check> checks) {
    this.checks = new ArrayList<>(checks);
  }

  /**
   * Adds a new {@link Check} to this runner
   *
   * @param check the {@link Check} to add
   */
  public void addCheck(Check check) {
    checks.add(check);
  }

  /**
   * Runs all checks against the given {@link CompiledSubmission}.
   *
   * @param compiledSubmission the {@link CompiledSubmission} to test
   * @return the {@link SubmissionCheckResult}
   */
  public SubmissionCheckResult checkSubmission(CompiledSubmission compiledSubmission) {
    Builder builder = ImmutableSubmissionCheckResult.builder();

    for (CompiledFile file : compiledSubmission.files()) {
      List<CheckResult> checkResults = checks.stream()
          .map(check -> tryCheck(check, file))
          .collect(toList());
      builder.putFileResults(file.qualifiedName(), checkResults);
    }

    builder.files(compiledSubmission.files());

    return builder.build();
  }

  private CheckResult tryCheck(Check check, CompiledFile file) {
    ErrorLogCapture capture = new ErrorLogCapture();
    try {
      capture.startCapture();
      Terminal.reset();
      return check.check(file);
    } catch (Throwable e) { // user checks should not crash everything
      return ImmutableCheckResult.builder()
          .message(findRootCause(e).getMessage())
          .check(check.name())
          .successful(false)
          .errorOutput(capture.getCaptured())
          .build();
    } finally {
      capture.stopCapture();
    }
  }

  private Throwable findRootCause(Throwable throwable) {
    if (throwable.getCause() != null) {
      return findRootCause(throwable.getCause());
    }
    return throwable;
  }
}