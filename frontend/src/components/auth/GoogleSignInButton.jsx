import { GoogleLogin } from "@react-oauth/google";

export default function GoogleSignInButton({ onCredential, onError }) {
    return (
        <GoogleLogin onSuccess={(response) => onCredential(response.credential)}
            onError={() => onError('No se pudo iniciar sesión con Google. Intenta de nuevo o usa tu correo.')}
            text="continue_with"
            locale="es" />
    )
}