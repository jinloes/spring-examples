async function getGreeting(): Promise<{ message: string }> {
  const res = await fetch("http://localhost:8080/api/hello", {
    cache: "no-store",
  });
  return res.json();
}

export default async function Home() {
  const { message } = await getGreeting();
  return (
    <main style={{ fontFamily: "sans-serif", padding: "2rem" }}>
      <h1>{message}</h1>
      <p>Served by Next.js · Powered by Spring Boot</p>
    </main>
  );
}