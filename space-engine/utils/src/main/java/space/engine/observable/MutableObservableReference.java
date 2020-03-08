package space.engine.observable;

import space.engine.barrier.Barrier;
import space.engine.barrier.functions.SupplierWithDelay;
import space.engine.baseobject.CanceledCheck;

/**
 * An {@link ObservableReference} which can be {@link #set(Object)} directly. There are 3 #set Methods available:
 * <ul>
 *     <li>{@link #set(Object)} sets the result to the parameter. May be canceled by another #set call.</li>
 *     <li>{@link #set(Generator)} sets the result to the result of the {@link Generator}. May be canceled by another #set call causing the {@link Generator} to never get called.</li>
 *     <li>{@link #set(GeneratorWithCancelCheck)} sets the result to the result of the {@link GeneratorWithCancelCheck} also supplying the {@link CanceledCheck}. Will <b>NOT</b> be canceled, always called and the result stored.</li>
 * </ul>
 */
public class MutableObservableReference<T> extends ObservableReference<T> {
	
	public MutableObservableReference() {
	}
	
	public MutableObservableReference(T initial) {
		super(initial);
	}
	
	public MutableObservableReference(SupplierWithDelay<T> supplier) {
		set(supplier);
	}
	
	public MutableObservableReference(Generator<T> supplier) {
		set(supplier);
	}
	
	public MutableObservableReference(GeneratorWithCancelCheck<T> supplier) {
		set(supplier);
	}
	
	//set
	public Barrier set(T t) {
		return ordering.next(prev -> prev.thenStartCancelable(
				canceledCheck -> setInternalAlways(t)
		));
	}
	
	public Barrier set(SupplierWithDelay<T> supplier) {
		return ordering.next(prev -> prev.thenStartCancelable(
				canceledCheck -> setInternalAlways(p -> supplier.get())
		));
	}
	
	public Barrier set(Generator<T> supplier) {
		return ordering.next(prev -> prev.thenStartCancelable(
				canceledCheck -> setInternalAlways(supplier)
		));
	}
	
	public Barrier set(GeneratorWithCancelCheck<T> supplier) {
		return ordering.next(prev -> prev.thenStartCancelable(
				canceledCheck -> setInternalAlways(supplier, canceledCheck)
		));
	}
	
	//setMayCancel
	public Barrier setMayCancel(SupplierWithDelay<T> supplier) {
		return ordering.next(prev -> prev.thenStartCancelable(
				canceledCheck -> setInternalMayCancel(p -> supplier.get(), canceledCheck)
		));
	}
	
	public Barrier setMayCancel(T t) {
		return ordering.next(prev -> prev.thenStartCancelable(
				canceledCheck -> setInternalMayCancel(t, canceledCheck)
		));
	}
	
	public Barrier setMayCancel(Generator<T> supplier) {
		return ordering.next(prev -> prev.thenStartCancelable(
				canceledCheck -> setInternalMayCancel(supplier, canceledCheck)
		));
	}
}