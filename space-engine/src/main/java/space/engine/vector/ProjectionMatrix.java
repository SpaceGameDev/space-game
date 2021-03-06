package space.engine.vector;

import static java.lang.Math.*;

public class ProjectionMatrix {
	
	/**
	 * same Matrix as gluPerspective()
	 */
	public static Matrix4 projection(float fov, float aspectRatio, float near, float far) {
		float scaleY = (float) tan(fov * 0.5 * PI / 180) * near;
		float scaleX = aspectRatio * scaleY;
		return frustum(-scaleX, scaleX, -scaleY, scaleY, near, far);
	}
	
	/**
	 * same Matrix as glFrustum()
	 */
	public static Matrix4 frustum(float left, float right, float bottom, float top, float near, float far) {
		return new Matrix4(
				2 * near / (right - left), 0, (right + left) / (right - left), 0,
				0, 2 * near / (top - bottom), (top + bottom) / (top - bottom), 0,
				0, 0, -((far + near) / (far - near)), -((2 * far * near) / (far - near)),
				0, 0, -1, 0
		);
	}
	
	/**
	 * same Matrix as glOrtho()
	 */
	public static Matrix4 ortho(float left, float right, float bottom, float top, float near, float far) {
		return new Matrix4(
				2 / (right - left), 0, 0, -((right + left) / (right - left)),
				0, 2 / (top - bottom), 0, -((top + bottom) / (top - bottom)),
				0, 0, -2 / (far - near), -((far + near) / (far - near)),
				0, 0, 0, 1
		);
	}
}
