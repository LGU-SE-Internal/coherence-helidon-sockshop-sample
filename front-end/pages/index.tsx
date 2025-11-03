// Copyright The OpenTelemetry Authors
// SPDX-License-Identifier: Apache-2.0

import Head from 'next/head';
import styled from 'styled-components';

const Container = styled.div`
  min-height: 100vh;
  padding: 0 0.5rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
`;

const Main = styled.main`
  padding: 5rem 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
`;

const Title = styled.h1`
  margin: 0;
  line-height: 1.15;
  font-size: 4rem;
  text-align: center;
  color: ${({ theme }) => theme.colors.otelBlue};
`;

const Description = styled.p`
  text-align: center;
  line-height: 1.5;
  font-size: 1.5rem;
  color: ${({ theme }) => theme.colors.textGray};
`;

export default function Home() {
  return (
    <Container>
      <Head>
        <title>Sock Shop - Microservices Demo</title>
        <meta name="description" content="Sock Shop microservices demo application" />
        <link rel="icon" href="/favicon.ico" />
      </Head>

      <Main>
        <Title>Welcome to Sock Shop</Title>
        <Description>
          A modern microservices demo application with OpenTelemetry observability
        </Description>
      </Main>
    </Container>
  );
}
