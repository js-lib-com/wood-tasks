package com.jslib.wood.tasks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.FileSystems;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.docore.IFiles;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.ITask;
import com.jslib.dospi.TaskAbortException;
import com.jslib.wood.tasks.util.TaskContext;

@RunWith(MockitoJUnitRunner.class)
public class CleanProjectTest {
	@Mock
	private TaskContext context;
	@Mock
	private IFiles files;
	@Mock
	private IParameters parameters;

	private ITask task;

	@Before
	public void beforeTest() throws TaskAbortException {
		when(files.getWorkingDir()).thenReturn(FileSystems.getDefault().getPath("."));
		when(files.exists(any())).thenReturn(true);
		when(files.getFileName(any())).thenReturn("test");

		task = new CleanProject(context, files);
	}

	@Test
	public void GivenContextProvided_WhenExecute_ThenCleanDirectory() throws Exception {
		// given
		when(context.getex("build.dir")).thenReturn("build");

		// when
		task.execute(parameters);

		// then
		verify(files, times(1)).cleanDirectory(any());
	}

	@Test
	public void GivenContextNotProvided_WhenExecute_ThenNotCleanDirectory() throws Exception {
		// given

		// when
		try {
			task.execute(parameters);
		} catch (Exception e) {
		}

		// then
		verify(files, times(0)).cleanDirectory(any());
	}

	@Test(expected = NullPointerException.class)
	public void GivenContextNotProvided_WhenExecute_ThenException() throws Exception {
		// given

		// when
		task.execute(parameters);

		// then
	}
}
