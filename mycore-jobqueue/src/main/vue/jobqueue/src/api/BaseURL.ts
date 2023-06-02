const mode = import.meta.env.MODE;

export const getBaseURL = () => {
    const isDev = mode === 'development';
    if(isDev){
        return "http://localhost:8291/mir/";
    } else {
        return (window as any).webApplicationBaseURL as string;
    }
}