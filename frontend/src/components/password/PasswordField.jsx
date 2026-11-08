import { useState } from 'react';
import styles from './PasswordPages.module.css';

export function CheckIcon({ size = 20 }) {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" aria-hidden="true">
            <circle cx={12} cy={12} r={12} fill="currentColor" />
            <path d="M7 12.5l3.2 3.2L17 9" fill="none" stroke="#FFF" strokeWidth={2.4} strokeLinecap="round" strokeLinejoin="round" />
        </svg>
    );
}

export function ShieldIcon() {
    return (
        <svg width={16} height={16} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            <path d="m9 12 2 2 4-4" />
        </svg>
    );
}

export default function PasswordField({ id, label, value, onChange, autoComplete, disabled, children }) {
    const [visible, setVisible] = useState(false);

    return (
        <div className={styles.field}>
            <label className={styles.label} htmlFor={id}>{label}</label>
            <div className={styles.control}>
                <input
                    id={id}
                    type={visible ? 'text' : 'password'}
                    className={styles.input}
                    value={value}
                    onChange={onChange}
                    autoComplete={autoComplete}
                    disabled={disabled}
                    required
                />
                <button
                    type="button"
                    className={styles.toggle}
                    onClick={() => setVisible(!visible)}
                    aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                >
                    {visible ? (
                        <svg width={18} height={18} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M9.88 9.88a3 3 0 1 0 4.24 4.24" /><path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68" /><path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61" /><line x1={2} x2={22} y1={2} y2={22} /></svg>
                    ) : (
                        <svg width={18} height={18} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" /><circle cx={12} cy={12} r={3} /></svg>
                    )}
                </button>
            </div>
            {children}
        </div>
    );
}